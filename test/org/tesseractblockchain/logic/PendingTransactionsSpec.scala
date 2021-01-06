package org.tesseractblockchain.logic

import java.time.Instant

import akka.util.ByteString
import cats.effect._
import core.Transaction
import core.Nonce
import core.fixtures.CoreFixtures
import core.Block
import tests.TestSpec
import zio._
import zio.interop.catz._
import zio.stm.{STM, TPriorityQueue, USTM, ZSTM}

class PendingTransactionsSpec extends TestSpec with CoreFixtures {

  private trait TestSetup {
    val sender = "048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb"
    val receiver = "057fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb"
    val amount = BigDecimal(1432.1234567123456789)
    val transactionFeeBasePrice = BigDecimal(0.00001)
    val transactionFeeLimit = BigDecimal(0.01)
  }

  "PendingTransactions#addPendingTransaction" must {
    "return add elements into queue" in new TestSetup {
      val queue: USTM[TPriorityQueue[Transaction]] = TPriorityQueue.empty[Transaction]
      val pt = PendingTransactions.apply(queue)
      val program: Task[List[Transaction]] = STM.atomically(for {
        // testing action:
        pt0   <- pt.addPendingTransaction(transaction1)
        // testing action:
        pt    <- pt0.addPendingTransaction(transaction2)
        queue <- pt.value
        txs   <- queue.toList
      } yield txs)

      whenReady(program)(_ mustBe Right(transaction2 :: transaction1 :: Nil))
    }
  }

  "PendingTransactions#pendingTransactionsAvailable" must {
    "return true when the queue is not empty" in new TestSetup {
      val queue: USTM[TPriorityQueue[Transaction]] = TPriorityQueue.empty[Transaction]
      val pt = PendingTransactions.apply(queue)
      val program = STM.atomically(for {
        pt0       <- pt.addPendingTransaction(transaction1)
        // testing action:
        predicate <- pt0.pendingTransactionsAvailable
      } yield predicate)

      whenReady(program)(_ mustBe Right(true))
    }
    "return false when the queue is empty" in new TestSetup {
      val queue: USTM[TPriorityQueue[Transaction]] = TPriorityQueue.empty[Transaction]
      val pt = PendingTransactions.apply(queue)

      whenReady(pt.pendingTransactionsAvailable.commit)(_ mustBe Right(false))
    }
  }

  "PendingTransactions#getTransactionsForNextBlock" must {
    "return all elements from queue for the next block" in {
      val queue: USTM[TPriorityQueue[Transaction]] = TPriorityQueue.empty[Transaction]
      val pt = PendingTransactions.apply(queue)
      val program: Task[List[Transaction]] = STM.atomically(for {
        pt0 <- pt.addPendingTransaction(transaction1)
        pt  <- pt0.addPendingTransaction(transaction2)
        // testing action:
        txs <- pt.getTransactionsForNextBlock
      } yield txs)

      whenReady(program)(_ mustBe Right(transaction1 :: transaction2 :: Nil))
    }
    "return all elements from queue (max 8)" in {
      val queue: USTM[TPriorityQueue[Transaction]] = TPriorityQueue.empty[Transaction]
      val pt = PendingTransactions.apply(queue)
      // [[SizeHelper.calculateTransactionCapacity]] mustBe 8, txs with 10 elements
      val txs: List[Transaction] = List(
        transaction1,
        transaction1,
        transaction1,
        transaction1,
        transaction1,
        transaction2,
        transaction2,
        transaction2,
        transaction2,
        transaction2
      )
      val expected: List[Transaction] = List(
        transaction1,
        transaction1,
        transaction1,
        transaction2,
        transaction2,
        transaction2,
        transaction2,
        transaction2
      )

      val program: Task[List[Transaction]] = STM.atomically(for {
        pt0 <- txs.foldLeft(ZSTM.succeed(pt))((pt, tx) => pt.flatMap(_.addPendingTransaction(tx)))
        // testing action:
        txs <- pt0.getTransactionsForNextBlock
      } yield txs)

      whenReady(program)(_ mustBe Right(expected))
    }
    "return nothing -- empty list of txs" in {
      val queue: USTM[TPriorityQueue[Transaction]] = TPriorityQueue.empty[Transaction]
      val pt = PendingTransactions.apply(queue)

      whenReady(STM.atomically(pt.getTransactionsForNextBlock))(_ mustBe Right(Nil))
    }
  }

  "PendingTransactions#clearPendingTransactions" must {
    "return all transaction their are not in a block" in new TestSetup {
      val queue = TPriorityQueue.empty[Transaction]
      val pt = PendingTransactions.apply(queue)
      val tx1 = Transaction.apply(sender, receiver, amount - 1 , Nonce(0), transactionFeeBasePrice, transactionFeeLimit)
      val tx2 = Transaction.apply(sender, receiver, amount - 2, Nonce(0), transactionFeeBasePrice, transactionFeeLimit)
      // [[SizeHelper.calculateTransactionCapacity]] mustBe 8, txs with 10 elements
      val txs0: List[Transaction] = (1 to 8).map { n =>
        Transaction.apply(sender, receiver, amount + n, Nonce(0), transactionFeeBasePrice, transactionFeeLimit)
      }.to(List)
      val txs = tx2 :: tx1 :: txs0
      val prevHash = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray

      val program: Task[List[Transaction]] = STM.atomically(for {
        pt0    <- txs.foldLeft(ZSTM.succeed(pt))((pt, tx) => pt.flatMap(_.addPendingTransaction(tx)))
        queue0 <- pt0.value.flatMap(_.toList)
        // testing action:
        pt     <- pt0.clearPendingTransactions(Block(prevHash, queue0.drop(2), Instant.now.toEpochMilli))
        queue  <- pt.value.flatMap(_.toList)
      } yield queue)

      whenReady(program) {
        case Right(l) =>
          val h1 = l.headOption
          val h2 = l.tail.headOption
          h1.map(_.transactionFeeLimit)     mustBe Some(tx1.transactionFeeLimit)
          h1.map(_.transactionFeeBasePrice) mustBe Some(tx1.transactionFeeBasePrice)
          h2.map(_.transactionFeeLimit)     mustBe Some(tx2.transactionFeeLimit)
          h2.map(_.transactionFeeBasePrice) mustBe Some(tx2.transactionFeeBasePrice)
        case Left(_) => fail("clearPendingTransactions")
      }
    }
  }

}
