package com.paymentstream.model

import zio.json._

/** Разбор батча JSON-строк (как после Kafka / колонка `json`) без Spark: те же правила, что
  * `filterPositiveAmount` / `nonPositiveAmount` на уже распарсенных строках.
  */
object TransactionJsonLines {

  final case class PartitionResult(
      accepted: Seq[Transaction],
      rejectedNonPositive: Seq[Transaction],
      invalidJson: Seq[String]
  ) {
    def totalRevenue: Long = accepted.foldLeft(0L)(_ + _.amount.toLong)
  }

  def partition(lines: Iterable[String]): PartitionResult = {
    val acceptedB = Seq.newBuilder[Transaction]
    val rejectedB = Seq.newBuilder[Transaction]
    val invalidB = Seq.newBuilder[String]
    lines.foreach { line =>
      line.fromJson[Transaction] match {
        case Right(t) if t.amount > 0  => acceptedB += t
        case Right(t)                   => rejectedB += t
        case Left(_)                    => invalidB += line
      }
    }
    PartitionResult(acceptedB.result(), rejectedB.result(), invalidB.result())
  }
}
