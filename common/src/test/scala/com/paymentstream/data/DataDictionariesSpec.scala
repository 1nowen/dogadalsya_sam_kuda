package com.paymentstream.data

import scala.util.Random

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class DataDictionariesSpec extends AnyFlatSpec with Matchers {

  behavior of "DataDictionaries.pick"

  it should "return an element from a non-empty sequence and reject an empty one" in {
    info("Summary: pick samples from non-empty lists; empty input throws.")
    val rng = new Random(99L)
    val seq = Seq("a", "b", "c")
    val picked = DataDictionaries.pick(seq)(rng)
    seq should contain(picked)

    val ex = intercept[IllegalArgumentException] {
      DataDictionaries.pick(Seq.empty[String])(rng)
    }
    ex.getMessage should include("empty")
  }
}
