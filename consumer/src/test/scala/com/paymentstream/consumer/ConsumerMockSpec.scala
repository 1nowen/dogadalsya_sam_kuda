package com.paymentstream.consumer

import java.time.Instant

import com.paymentstream.model.{Transaction, TransactionJsonLines}
import org.mockito.Mockito.when
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.json._

/** Контракт «батч JSON из Kafka» — мокируется вместо реального брокера / Spark. */
private[consumer] trait KafkaJsonBatchSource {
  def fetch(): Seq[String]
}

final class ConsumerMockSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  it should "accept positive amounts, reject non-positive and invalid JSON from mocked Kafka batch" in {
    val good = Transaction(
      user = "u1",
      productType = "physical",
      eventType = "purchase",
      category = "books",
      amount = 10,
      timestamp = Instant.parse("2026-01-15T12:30:00Z")
    )
    val zero = Transaction(
      user = "u2",
      productType = "physical",
      eventType = "purchase",
      category = "books",
      amount = 0,
      timestamp = Instant.parse("2026-01-15T00:00:00Z")
    )

    val source = mock[KafkaJsonBatchSource]
    when(source.fetch()).thenReturn(
      Seq(
        good.toJson,
        zero.toJson,
        """{"not":"a transaction"}"""
      )
    )

    val result = TransactionJsonLines.partition(source.fetch())
    result.accepted shouldBe Seq(good)
    result.rejectedNonPositive shouldBe Seq(zero)
    result.invalidJson shouldBe Seq("""{"not":"a transaction"}""")
    result.totalRevenue shouldBe 10L
  }
}
