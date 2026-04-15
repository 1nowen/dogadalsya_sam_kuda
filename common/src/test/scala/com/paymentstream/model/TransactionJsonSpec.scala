package com.paymentstream.model

import java.time.Instant

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.json._

final class TransactionJsonSpec extends AnyFlatSpec with Matchers {

  behavior of "Transaction zio-json codec"

  it should "round-trip encode and decode a sample transaction" in {
    info("Summary: JSON encoder and decoder preserve all fields.")
    val original = Transaction(
      user = "user_001",
      productType = "physical",
      eventType = "purchase",
      category = "books",
      amount = 42,
      timestamp = Instant.parse("2026-01-15T12:30:00Z")
    )
    val json = original.toJson
    json.fromJson[Transaction] shouldBe Right(original)
  }
}
