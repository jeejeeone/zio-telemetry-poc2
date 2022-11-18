package com.example

import com.example.telemetry.JaegerTracer
import zio._
import zio.http._
import zio.http.model.Method
import zio.telemetry.opentelemetry.Tracing

import java.net.HttpURLConnection

/*
 Simply adding opentelemetry java agent will render span hello-fetch outside of hello trace in jaeger.
 Furthermore traces from autoinstrumentation are not included in root span. Remove java agent to compare results.

 Reproduce:
   - Run jaeger

      docker run -d --name jaeger \
        -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 \
        -e COLLECTOR_OTLP_ENABLED=true \
        -p 6831:6831/udp \
        -p 6832:6832/udp \
        -p 5778:5778 \
        -p 16686:16686 \
        -p 4317:4317 \
        -p 4318:4318 \
        -p 14250:14250 \
        -p 14268:14268 \
        -p 14269:14269 \
        -p 9411:9411 \
        jaegertracing/all-in-one:1.38
   - Run HelloWorld
     - Use javaagent: -javaagent:/xx/opentelemetry-javaagent.jar
     - Use env variables: OTEL_SERVICE_NAME=example-app;OTEL_TRACES_EXPORTER=jaeger;OTEL_EXPORTER_JAEGER_ENDPOINT=http://localhost:14250;OTEL_METRICS_EXPORTER=none
   - curl localhost:8080/hello
   - See trace in jaeger http://localhost:16686/search?service=example-app
 */

// Note! Identical results with two different Jaeger tracers one from zio-telemetry and one from
// https://kadek-marek.medium.com/trace-your-microservices-with-zio-telemetry-5f88d69cb26b

object HelloWorld extends ZIOAppDefault {
  // Something to test autoinstrumentation
  def fetch =
    ZIO.attempt {
      val url = new java.net.URL("https://www.google.fi/")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      con.setRequestMethod("GET")
      con.getResponseCode
    }.orDie

  val app = Http.collectZIO[Request] { case Method.GET -> !! / "hello" =>
    ZIO.serviceWithZIO[Tracing] { tracing =>
      import tracing.aspects._

      (fetch.flatMap(code => ZIO.succeed(Response.text(s"Hello World! $code"))) @@ span("hello-fetch")) @@ root("hello")
    }
  }

  override val run =
    // Same result with JaegerTracer2.live
    Server.serve(app).provide(Server.default, Tracing.propagating, JaegerTracer.live)
}
