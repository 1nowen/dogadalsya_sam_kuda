package com.paymentstream.config

import scala.concurrent.duration.FiniteDuration

final case class KafkaProducerParams(acks: String, retries: Int)

final case class KafkaConfig(
  bootstrapServers: String,
  topic: String,
  consumerGroup: String,
  producer: KafkaProducerParams
)

final case class ProducerConfig(generationInterval: FiniteDuration)

final case class SparkConfig(
  master: String,
  appName: String,
  checkpointLocation: String,
  rejectedTransactionsPath: String,
  transactionsLogPath: String,
  triggerInterval: FiniteDuration,
  startingOffsets: String,
  logLevel: String
)

// Корень конфигурации PureConfig: в HOCON секции kafka (включая kafka.producer), producer и spark.
final case class AppConfig(kafka: KafkaConfig, producer: ProducerConfig, spark: SparkConfig)
