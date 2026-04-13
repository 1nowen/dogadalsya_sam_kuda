package com.paymentstream.consumer

import com.paymentstream.config.ConfigLoader
import com.paymentstream.spark.{SparkConsumerConfig, TransactionStreamProcessor}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, concat_ws, current_timestamp, lit}
import org.apache.spark.sql.streaming.Trigger

object ConsumerMain {

  // Склеивает поля транзакции в одну строку (разделитель " | ") и дописывает в текстовый лог Spark.
  private def appendAllTransactionsToLog(
    transactionsDataFrame: DataFrame,
    logDirectoryPath: String
  ): Unit =
    transactionsDataFrame
      .select(
        concat_ws(
          " | ",
          col("user"),
          col("productType"),
          col("eventType"),
          col("category"),
          col("amount").cast("string"),
          col("timestamp").cast("string")
        )
      )
      .write
      .mode("append")
      .text(logDirectoryPath)

  def main(args: Array[String]): Unit = {
    val applicationConfig = ConfigLoader.loadOrThrow()
    val sparkConsumerConfig = SparkConsumerConfig.fromAppConfig(applicationConfig)

    // Shutdown
    val sparkSession = TransactionStreamProcessor.sparkSession(sparkConsumerConfig)
    sys.addShutdownHook {
      sparkSession.stop()
    }

    // Kafka → колонка value как строка JSON; затем разбор схемы и приведение timestamp к типу Spark.
    val kafkaStreamDataFrame =
      TransactionStreamProcessor.kafkaStream(sparkSession, sparkConsumerConfig)
    val transactions = TransactionStreamProcessor.castEventTimestamp(
      TransactionStreamProcessor.parseTransactions(kafkaStreamDataFrame)
    )

    // Чекпойнт хранит offset'ы; триггер задаёт размер микро-батча по времени.
    val streamingQuery = transactions.writeStream
      .outputMode("append")
      .option("checkpointLocation", sparkConsumerConfig.checkpointLocation)
      .trigger(Trigger.ProcessingTime(sparkConsumerConfig.triggerInterval.toMillis))
      .foreachBatch { (microBatchDataFrame: DataFrame, _: Long) =>
        // Кэш: один батч многократно читается (лог, фильтры, count); в конце обязательно unpersist.
        val cachedMicroBatch = microBatchDataFrame.cache()
        try {
          // 1) Полный след батча в текстовый лог.
          appendAllTransactionsToLog(
            cachedMicroBatch,
            sparkConsumerConfig.transactionsLogPath
          )
          val rejectedTransactions =
            TransactionStreamProcessor.nonPositiveAmount(cachedMicroBatch).cache()
          val acceptedTransactions =
            TransactionStreamProcessor.filterPositiveAmount(cachedMicroBatch).cache()
          try {
            // 2) Проблемные строки (amount null или ≤ 0) — в JSON
            rejectedTransactions.write.mode(
              "append"
            ).json(sparkConsumerConfig.rejectedTransactionsPath)
            val incorrectTransactionCount = rejectedTransactions.count()
            val correctTransactionCount = acceptedTransactions.count()
            // 3) Детализация по группам (без счётчиков батча).
            val aggregationSummary = TransactionStreamProcessor.aggregate(acceptedTransactions)
            aggregationSummary.show(false)

            // Итог по батчу: выручка и счётчики корректных/некорректных строк.
            val totals = TransactionStreamProcessor
              .aggregateTotals(acceptedTransactions)
              .withColumn("correctTransactionCount", lit(correctTransactionCount))
              .withColumn("incorrectTransactionCount", lit(incorrectTransactionCount))
              .withColumn("summaryAt", current_timestamp())
            totals.show(false)
          } finally {
            rejectedTransactions.unpersist()
            acceptedTransactions.unpersist()
          }
        } finally {
          cachedMicroBatch.unpersist()
        }
      }
      .start()

    streamingQuery.awaitTermination()
  }
}
