package com.paymentstream.model

import java.time.Instant

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class Transaction(
  user: String,
  productType: String,
  eventType: String,
  category: String,
  amount: Int,
  timestamp: Instant
)

object Transaction {
  implicit val encoder: JsonEncoder[Transaction] = DeriveJsonEncoder.gen[Transaction]
  implicit val decoder: JsonDecoder[Transaction] = DeriveJsonDecoder.gen[Transaction]
}
