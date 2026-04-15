package com.paymentstream.generator

import scala.util.Random

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.paymentstream.data.DataDictionaries

final class TransactionGeneratorSpec extends AnyFlatSpec with Matchers {

  behavior of "TransactionGenerator.buildTransaction"

  it should "create a valid Transaction (dictionaries, amount range, timestamp present)" in {
    info("Summary: one generated row matches domain rules for many seeds.")
    (0 until 200).foreach { seed =>
      val t = TransactionGenerator.buildTransaction(new Random(seed.toLong))
      DataDictionaries.users should contain(t.user)
      DataDictionaries.productTypes should contain(t.productType)
      DataDictionaries.eventTypes should contain(t.eventType)
      DataDictionaries.productCategories.getOrElse(t.productType, Seq.empty) should contain(t.category)
      if (t.amount == 0) succeed
      else t.amount should (be >= 1 and be <= 1000)
      t.timestamp should not be null
    }
  }
}
