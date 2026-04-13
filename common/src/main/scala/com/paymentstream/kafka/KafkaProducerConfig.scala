package com.paymentstream.kafka

import com.paymentstream.config.AppConfig
import org.apache.kafka.clients.producer.ProducerConfig
import zio.kafka.producer.ProducerSettings

final case class KafkaProducerConfig(
  bootstrapServers: List[String],
  topic: String,
  acks: String,
  retries: Int
)

object KafkaProducerConfig {

  // Достаёт параметры из AppConfig: bootstrap через запятую, топик и настройки надёжности продьюсера.
  def fromAppConfig(applicationConfig: AppConfig): KafkaProducerConfig = {
    val kafkaConfig = applicationConfig.kafka
    val bootstrapServerAddresses =
      kafkaConfig.bootstrapServers.split(",").map(_.trim).filter(_.nonEmpty).toList
    require(bootstrapServerAddresses.nonEmpty, "kafka.bootstrap-servers must not be empty")
    KafkaProducerConfig(
      bootstrapServers = bootstrapServerAddresses,
      topic = kafkaConfig.topic,
      acks = kafkaConfig.producer.acks,
      retries = kafkaConfig.producer.retries
    )
  }

  def producerSettings(kafkaProducerConfig: KafkaProducerConfig): ProducerSettings =
    ProducerSettings(kafkaProducerConfig.bootstrapServers)
      .withProperty(ProducerConfig.ACKS_CONFIG, kafkaProducerConfig.acks)
      .withProperty(ProducerConfig.RETRIES_CONFIG, Int.box(kafkaProducerConfig.retries))
}
