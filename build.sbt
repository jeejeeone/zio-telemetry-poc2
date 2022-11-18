ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "zio-telemetry-poc2",
    libraryDependencies ++= Seq(
      "dev.zio"         %% "zio"                           % "2.0.3",
      "dev.zio"         %% "zio-http"                      % "0.0.3",
      "dev.zio"         %% "zio-opentelemetry"             % "3.0.0-RC1",
      "io.opentelemetry" % "opentelemetry-exporter-jaeger" % "1.6.0",
      "io.opentelemetry" % "opentelemetry-sdk"             % "1.6.0",
      "io.grpc"          % "grpc-netty-shaded"             % "1.40.1",
      "dev.zio"         %% "zio-test"                      % "2.0.3" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
