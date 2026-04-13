import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._

ThisBuild / organization := "com.paymentstream"
ThisBuild / scalaVersion := Versions.scalaVersion
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / evictionErrorLevel := sbt.util.Level.Warn

// Для Spark 3.5 на Java 17
lazy val sparkDockerJavaOptions: Seq[String] = Seq(
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
  "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
  "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED"
)

lazy val dockerAppBaseSettings = Seq(
  dockerBaseImage := "eclipse-temurin:17-jre-jammy",
  dockerRepository := None,
  dockerUpdateLatest := true,
  Docker / packageName := name.value
)

lazy val common = (project in file("common"))
  .settings(
    name := "payment-common",
    libraryDependencies ++= Dependencies.common ++ Dependencies.test
  )

lazy val producerApp = (project in file("producer"))
  .dependsOn(common)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "producer",
    Compile / mainClass := Some("com.paymentstream.producer.ProducerMain")
  )
  .settings(dockerAppBaseSettings: _*)
  .settings(dockerEnvVars := Map("CONFIG_FILE" -> "/app/config/application.conf"))

lazy val consumerApp = (project in file("consumer"))
  .dependsOn(common)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "consumer",
    libraryDependencies ++= Dependencies.sparkStack,
    Compile / mainClass := Some("com.paymentstream.consumer.ConsumerMain")
  )
  .settings(dockerAppBaseSettings: _*)
  .settings(dockerEnvVars := Map(
    "CONFIG_FILE" -> "/app/config/application.conf",
    "JAVA_TOOL_OPTIONS" -> sparkDockerJavaOptions.mkString(" ")
  ))

lazy val root = (project in file("."))
  .aggregate(common, producerApp, consumerApp)
  .settings(
    name := "payment-transaction-simulation",
    publish / skip := true
  )
