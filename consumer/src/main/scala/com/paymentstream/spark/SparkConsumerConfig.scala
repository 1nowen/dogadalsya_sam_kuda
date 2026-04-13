package com.paymentstream.spark

import scala.concurrent.duration.FiniteDuration

import com.paymentstream.config.AppConfig
import org.apache.spark.sql.types._

final case class SparkConsumerConfig(
  master: String,
  appName: String,
  checkpointLocation: String,
  rejectedTransactionsPath: String,
  transactionsLogPath: String,
  triggerInterval: FiniteDuration,
  startingOffsets: String,
  logLevel: String,
  kafkaBootstrapServers: String,
  kafkaTopic: String,
  kafkaConsumerGroup: String
)

object SparkConsumerConfig {

  // Объединяет секции spark и kafka из AppConfig в один набор опций для драйвера и источника Kafka.
  def fromAppConfig(applicationConfig: AppConfig): SparkConsumerConfig = {
    val kafkaConfig = applicationConfig.kafka
    val sparkConfig = applicationConfig.spark
    SparkConsumerConfig(
      master = sparkConfig.master,
      appName = sparkConfig.appName,
      checkpointLocation = sparkConfig.checkpointLocation,
      rejectedTransactionsPath = sparkConfig.rejectedTransactionsPath,
      transactionsLogPath = sparkConfig.transactionsLogPath,
      triggerInterval = sparkConfig.triggerInterval,
      startingOffsets = sparkConfig.startingOffsets,
      logLevel = sparkConfig.logLevel,
      kafkaBootstrapServers = kafkaConfig.bootstrapServers,
      kafkaTopic = kafkaConfig.topic,
      kafkaConsumerGroup = kafkaConfig.consumerGroup
    )
  }

  object TransactionJsonSchema {

    // Поля совпадают с JSON, который пишет продьюсер (zio-json для Transaction).
    val structType: StructType = StructType(Seq(
      StructField("user", StringType),
      StructField("productType", StringType),
      StructField("eventType", StringType),
      StructField("category", StringType),
      StructField("amount", IntegerType),
      StructField("timestamp", StringType)
    ))
  }
}
