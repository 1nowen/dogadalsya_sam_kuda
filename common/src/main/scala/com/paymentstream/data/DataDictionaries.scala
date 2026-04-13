package com.paymentstream.data

import scala.util.Random

object DataDictionaries {

  val users: Seq[String] = Seq(
    "user_001",
    "user_002",
    "user_003",
    "user_004",
    "user_005",
    "user_006",
    "user_007",
    "user_008",
    "user_009",
    "user_010"
  )

  val productTypes: Seq[String] = Seq("physical", "digital", "service", "subscription")

  val eventTypes: Seq[String] = Seq("purchase", "refund", "chargeback", "inquiry")

  val productCategories: Map[String, Seq[String]] = Map(
    "physical" -> Seq("clothing", "electronics", "furniture", "books", "toys"),
    "digital" -> Seq("software", "e-book", "music", "movie", "game"),
    "service" -> Seq("consulting", "repair", "delivery", "cleaning"),
    "subscription" -> Seq("streaming", "news", "cloud_storage", "membership")
  )

  def pick[A](values: Seq[A])(implicit randomNumberGenerator: Random): A = {
    require(values.nonEmpty, "pick: empty dictionary")
    values(randomNumberGenerator.nextInt(values.size))
  }
}
