package com.paymentstream.spark

import scala.collection.JavaConverters._

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class TransactionStreamProcessorSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private var spark: SparkSession = _

  override def beforeAll(): Unit = {
    spark = SparkSession
      .builder()
      .appName("TransactionStreamProcessorSpec")
      .master("local[1]")
      .config("spark.ui.enabled", "false")
      .getOrCreate()
    spark.sparkContext.setLogLevel("WARN")
  }

  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
      spark = null
    }
    SparkSession.clearActiveSession()
    SparkSession.clearDefaultSession()
  }

  behavior of "TransactionStreamProcessor.filterPositiveAmount"

  it should "keep only rows where amount is greater than zero" in {
    info("Summary: null and non-positive amounts are dropped.")
    val amountSchema = StructType(Seq(StructField("amount", IntegerType, nullable = true)))
    val rows = Seq(Row(0), Row(5), Row(-1), Row(null))
    val df = spark.createDataFrame(rows.asJava, amountSchema)
    val out = TransactionStreamProcessor.filterPositiveAmount(df).collect().map(_.getInt(0)).toSet
    out shouldBe Set(5)
  }

  behavior of "TransactionStreamProcessor.aggregateTotals"

  it should "sum amount into totalRevenue for the batch" in {
    info("Summary: aggregateTotals matches arithmetic sum of amount column.")
    val amountSchema = StructType(Seq(StructField("amount", IntegerType, nullable = false)))
    val rows = Seq(Row(10), Row(20), Row(5))
    val df = spark.createDataFrame(rows.asJava, amountSchema)
    val row = TransactionStreamProcessor.aggregateTotals(df).collect()(0)
    row.getAs[Any]("totalRevenue") match {
      case v: Long   => v shouldBe 35L
      case v: Int    => v shouldBe 35
      case v: BigInt => v.toLong shouldBe 35L
      case other     => fail(s"unexpected type for totalRevenue: $other")
    }
  }
}
