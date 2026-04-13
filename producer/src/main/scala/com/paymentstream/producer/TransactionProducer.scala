package com.paymentstream.producer

import com.paymentstream.config.AppConfig
import com.paymentstream.generator.TransactionGenerator
import com.paymentstream.kafka.KafkaProducerConfig
import zio._
import zio.durationInt
import zio.json._
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde

object TransactionProducer {

  // Стартовый интервал для backoff между повторными отправками в Kafka.
  private val RetryInitialDelay: Duration = 100.millis

  // Бесконечный цикл: сгенерировать транзакцию, отправить в топик, при сбое — retry с backoff до retries раз.
  def run(applicationConfig: AppConfig): ZIO[Producer, Throwable, Unit] = {
    val kafkaProducerConfig = KafkaProducerConfig.fromAppConfig(applicationConfig)
    val topic = kafkaProducerConfig.topic
    val interval = Duration.fromScala(applicationConfig.producer.generationInterval)

    val retryPolicy = Schedule.exponential(RetryInitialDelay) &&
      Schedule.recurs(kafkaProducerConfig.retries)

    val sendOnce: ZIO[Producer, Throwable, Unit] = TransactionGenerator.generate
      .flatMap { transaction =>
        val jsonPayload: String = transaction.toJson

        Producer
          .produce(topic, transaction.user, jsonPayload, Serde.string, Serde.string)
          .tapError(throwable =>
            ZIO.logWarning(s"Produce attempt failed for $topic: ${throwable.getMessage}")
          )
          .retry(retryPolicy)
          .tapError(throwable =>
            ZIO.logError(
              s"Produce failed after ${kafkaProducerConfig.retries} retries for $topic: ${throwable.getMessage}"
            )
          ) *>
          ZIO.logInfo(s"Sent to $topic: $jsonPayload")
      }

    sendOnce.repeat(Schedule.fixed(interval)).unit
  }
}
