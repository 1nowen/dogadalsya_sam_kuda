package com.paymentstream.producer

import com.paymentstream.config.ConfigLoader
import com.paymentstream.kafka.KafkaProducerConfig
import zio._
import zio.kafka.producer.Producer

object ProducerMain extends ZIOAppDefault {

  // Загрузка конфига → настройки Kafka → ZIO Producer в scope → цикл генерации и публикации в TransactionProducer.
  override def run: ZIO[Any, Throwable, Unit] = ZIO.fromEither(ConfigLoader.load())
    .mapError(failures => new IllegalArgumentException(failures.prettyPrint())).flatMap {
      applicationConfig =>
        val kafkaProducerConfig = KafkaProducerConfig.fromAppConfig(applicationConfig)
        val producerSettings = KafkaProducerConfig.producerSettings(kafkaProducerConfig)
        ZIO.scoped {
          Producer.make(producerSettings).flatMap { producer =>
            TransactionProducer.run(applicationConfig).provideLayer(ZLayer.succeed(producer))
          }
        }
    }
}
