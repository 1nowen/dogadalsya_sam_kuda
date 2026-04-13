package com.paymentstream.spark

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, from_json, max, sum}

object TransactionStreamProcessor {

  // Создаёт или переиспользует глобальную SparkSession и задаёт уровень логов драйвера.
  def sparkSession(sparkConsumerConfig: SparkConsumerConfig): SparkSession = {
    val builtSparkSession = SparkSession
      .builder()
      .appName(sparkConsumerConfig.appName)
      .master(sparkConsumerConfig.master)
      .getOrCreate()
    builtSparkSession.sparkContext.setLogLevel(sparkConsumerConfig.logLevel)
    builtSparkSession
  }

  // Structured Streaming из Kafka: value как JSON-строка. failOnDataLoss=false — при сбросе топика не падать из‑за старого checkpoint.
  def kafkaStream(sparkSession: SparkSession, sparkConsumerConfig: SparkConsumerConfig): DataFrame =
    sparkSession.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", sparkConsumerConfig.kafkaBootstrapServers)
      .option("subscribe", sparkConsumerConfig.kafkaTopic)
      .option("startingOffsets", sparkConsumerConfig.startingOffsets)
      .option("kafka.group.id", sparkConsumerConfig.kafkaConsumerGroup)
      .option("failOnDataLoss", "false")
      .load()
      .select(col("value").cast("string").as("json"))

  // Разбор JSON в колонки по фиксированной схеме (см. SparkConsumerConfig.TransactionJsonSchema).
  def parseTransactions(kafkaJsonDataFrame: DataFrame): DataFrame =
    kafkaJsonDataFrame
      .select(
        from_json(col("json"), SparkConsumerConfig.TransactionJsonSchema.structType)
          .as("transaction")
      )
      .select("transaction.*")

  // В JSON timestamp приходит строкой; для фильтров и окон приводим к timestamp Spark через cast.
  def castEventTimestamp(dataFrame: DataFrame): DataFrame =
    dataFrame.withColumn("timestamp", col("timestamp").cast("timestamp"))

  def filterPositiveAmount(dataFrame: DataFrame): DataFrame =
    dataFrame.filter(col("amount") > 0)

  def nonPositiveAmount(dataFrame: DataFrame): DataFrame =
    dataFrame.filter(col("amount").isNull || col("amount") <= 0)

  // Сумма amount по группе; время регистрации — максимальный timestamp в группе (при одной строке совпадает с ней).
  def aggregate(dataFrame: DataFrame): DataFrame =
    dataFrame
      .groupBy("user", "productType", "eventType", "category")
      .agg(
        sum("amount").as("totalAmount"),
        max("timestamp").as("transactionRegisteredAt")
      )

  def aggregateTotals(dataFrame: DataFrame): DataFrame =
    dataFrame.agg(sum("amount").as("totalRevenue"))
}
