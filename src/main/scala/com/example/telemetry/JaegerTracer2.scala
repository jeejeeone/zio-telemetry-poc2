package com.example

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace._
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.{TextMapGetter, TextMapSetter}
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace._
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import zio._

import scala.collection.mutable
import scala.jdk.CollectionConverters._

// From https://kadek-marek.medium.com/trace-your-microservices-with-zio-telemetry-5f88d69cb26b

object JaegerTracer2 {
  val errorMapper: PartialFunction[Throwable, StatusCode] = { case _ => StatusCode.ERROR }

  val propagator                           = W3CTraceContextPropagator.getInstance()
  val carrier: mutable.Map[String, String] = mutable.Map().empty

  val getter: TextMapGetter[mutable.Map[String, String]] = new TextMapGetter[mutable.Map[String, String]] {
    override def keys(carrier: mutable.Map[String, String]): java.lang.Iterable[String] =
      carrier.keys.asJava

    override def get(carrier: mutable.Map[String, String], key: String): String =
      carrier.get(key).orNull
  }

  val setter: TextMapSetter[mutable.Map[String, String]] =
    (carrier, key, value) => carrier.update(key, value)

  def make(serviceName: String): TaskLayer[Tracer] = ZLayer.scoped {
    val serviceNameResource =
      Resource.create(
        Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)
      )
    for {
      spanExporter   <- ZIO.fromAutoCloseable(
                          ZIO.attempt(
                            JaegerGrpcSpanExporter
                              .builder()
                              .setEndpoint("http://127.0.0.1:14250")
                              .build()
                          )
                        )
      spanProcessor  <- ZIO.fromAutoCloseable(ZIO.succeed(SimpleSpanProcessor.create(spanExporter)))
      tracerProvider <- ZIO.fromAutoCloseable(
                          ZIO.succeed(
                            SdkTracerProvider
                              .builder()
                              .addSpanProcessor(spanProcessor)
                              .setResource(serviceNameResource)
                              .build()
                          )
                        )
      openTelemetry  <- ZIO
                          .succeed(
                            OpenTelemetrySdk
                              .builder()
                              .setTracerProvider(tracerProvider)
                              .build()
                          )
      tracer         <- ZIO
                          .succeed(
                            openTelemetry.getTracer("zio.telemetry.opentelemetry")
                          )
    } yield tracer
  }
}
