package core

import core.exceptions.TransactionNotFoundException
import zio.Task

final case class TransactionCache(value: List[(String, Transaction)]) extends AnyVal

object TransactionCache {

  implicit class RichTransactionCache(transactionCache: TransactionCache) {
    def add(hex: String, block: Transaction): TransactionCache =
      TransactionCache((hex, block) :: transactionCache.value)

    def get(hex: String): Task[Transaction] = {
      transactionCache.value.find({ case (key, _) => key == hex }) match {
        case Some((_, transaction)) => Task.succeed(transaction)
        case None => Task.fail(TransactionNotFoundException(hex))
      }
    }
  }

}
