package com.paymentstream.generator

import java.time.Instant
import scala.util.Random

import zio.{UIO, ZIO}

import com.paymentstream.data.DataDictionaries
import com.paymentstream.model.Transaction

object TransactionGenerator {

  /** Deterministic transaction from a RNG (used by `generate` and unit tests). */
  def buildTransaction(randomNumberGenerator: Random): Transaction = {
    val productType = DataDictionaries.pick(DataDictionaries.productTypes)(randomNumberGenerator)
    val categories = DataDictionaries.productCategories(productType)
    val amount =
      if (randomNumberGenerator.nextDouble() < 0.1) 0
      else randomNumberGenerator.nextInt(1000) + 1

    Transaction(
      user = DataDictionaries.pick(DataDictionaries.users)(randomNumberGenerator),
      productType = productType,
      eventType = DataDictionaries.pick(DataDictionaries.eventTypes)(randomNumberGenerator),
      category = DataDictionaries.pick(categories)(randomNumberGenerator),
      amount = amount,
      timestamp = Instant.now()
    )
  }

  // ~10% с amount = 0 (некорректная транзакция для consumer / incorrectTransactionCount).
  def generate: UIO[Transaction] = ZIO.succeed(buildTransaction(new Random()))
}
