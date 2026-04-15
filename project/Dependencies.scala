import sbt._

object Versions {
  val scalaVersion = "2.13.15"
  val zio = "2.1.14"
  val zioKafka = "2.8.3"
  val zioJson = "0.6.2"
  val spark = "3.5.4"
  val pureconfig = "0.17.7"
  val scalaTest = "3.2.19"
  val mockitoScala = "1.17.37"
  val log4j = "2.24.3"
  val slf4jNop = "1.7.36"
}

object Dependencies {
  import Versions._

  val common: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio" % zio,
    "dev.zio" %% "zio-kafka" % zioKafka,
    "dev.zio" %% "zio-json" % zioJson,
    "com.github.pureconfig" %% "pureconfig" % pureconfig
  )

  private val sparkLogging: Seq[ModuleID] = Seq(
    "org.apache.logging.log4j" % "log4j-api" % log4j,
    "org.apache.logging.log4j" % "log4j-core" % log4j,
    "org.apache.logging.log4j" % "log4j-slf4j2-impl" % log4j
  )

  // Consumer: Spark SQL, Kafka.
  val sparkStack: Seq[ModuleID] =
    Seq(
      "org.apache.spark" %% "spark-sql" % spark,
      "org.apache.spark" %% "spark-sql-kafka-0-10" % spark
    ) ++ sparkLogging

//заглушка SLF4J для kafka-clients.
  val producerSlf4j: Seq[ModuleID] = Seq(
    "org.slf4j" % "slf4j-nop" % slf4jNop
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % scalaTest % Test
  )

  val testWithMockito: Seq[ModuleID] =
    test :+ ("org.mockito" %% "mockito-scala" % mockitoScala % Test)
}
