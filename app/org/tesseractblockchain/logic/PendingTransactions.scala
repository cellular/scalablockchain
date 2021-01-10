package org.tesseractblockchain.logic

import cats.effect._
import cats.implicits._
import core.util.SizeHelper
import core.util.categorytheory.ZioComonad._
import core.{Block, Transaction}
import zio.interop.catz._
import zio.stm.{TPriorityQueue, USTM, ZSTM}

private[tesseractblockchain] final case class PendingTransactions(value: USTM[TPriorityQueue[Transaction]])

private[tesseractblockchain] object PendingTransactions {

  def apply: PendingTransactions = PendingTransactions(value = TPriorityQueue.empty[Transaction])

  implicit class RichPendingTransactions(pendingTransactions: PendingTransactions) {
    def addPendingTransaction(tx: Transaction): ZSTM[Any, Nothing, PendingTransactions] =
      for {
        queue <- pendingTransactions.value
        _     <- queue.offer(tx)
      } yield pendingTransactions.copy(value = ZSTM.succeed(queue))

    def getTransactionsForNextBlock: USTM[List[Transaction]] =
      pendingTransactions.value.flatMap { queue =>
        (0 until SizeHelper.calculateTransactionCapacity)
          .foldLeft(ZSTM.succeed(Nil): USTM[List[Transaction]]) { (txs, _) =>
            queue.takeOption.flatMap {
              case Some(tx) => txs.map(tx :: _)
              case None => txs
            }
          }.map(_.reverse)
      }

    def clearPendingTransactions(transactions: List[Transaction]): USTM[PendingTransactions] =
      for {
        queue <- pendingTransactions.value
        diff  <- queue.toList.map(diffTxs(_, transactions))
        _     <- queue.removeIf(_ => true) *> queue.offerAll(diff)
      } yield pendingTransactions.copy(value = ZSTM.succeed(queue))

    private def diffTxs(queue: List[Transaction], transactions: List[Transaction]): List[Transaction] =
      queue.filterNot(tx1 => transactions.exists(tx2 => tx1.equals(tx2.some).extract))

    def clearPendingTransactions(block: Block): USTM[PendingTransactions] = clearPendingTransactions(block.transactions)

    def pendingTransactionsAvailable: USTM[Boolean] = pendingTransactions.value.>>=(_.toList.map(_.nonEmpty))
  }

}
