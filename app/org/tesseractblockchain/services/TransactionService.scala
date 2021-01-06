package org.tesseractblockchain.services

import cats.implicits._
import cats.Applicative
import cats.effect._
import core.exceptions.TransactionNotFoundException
import core.{BlockSize, Offset, Transaction}
import javax.inject.Inject
import org.tesseractblockchain.DependencyManager
import zio._
import zio.interop.catz.taskConcurrentInstance
import zio.stm.STM

private[tesseractblockchain] class TransactionService @Inject()() {

  def sendTransaction(tx: Transaction): ZIO[DependencyManager, Throwable, Unit] =
    ZIO.accessM[DependencyManager] { dm =>
      for {
        pendingTransactionsRef <- dm.pendingTransactions
        pendingTransactions    <- pendingTransactionsRef.get
        _                      <- Applicative[Task].map2(
                                 STM.atomically(pendingTransactions.addPendingTransaction(tx)),
                                 dm.miner0.>>=(_.update(_.map(_.copy(canceledBlock = true))))
                               )((ptx, _) => pendingTransactionsRef.set(ptx))
      } yield ()
    }

  def getTransactionByHash(hex: String): ZIO[DependencyManager, Throwable, Transaction] =
    ZIO
      .accessM[DependencyManager](_.blockchain.getInstance(_.getTransactionByHash(hex)))
      .flatten
      .mapError(_ => TransactionNotFoundException(hex))

  def getRecentTransactions(size: BlockSize, offset: Offset): ZIO[DependencyManager, Throwable, List[Transaction]] =
    ZIO
      .accessM[DependencyManager](_.blockchain.getInstance(_.getLatestBlocks(size, offset).map(_.>>=(_.transactions))))
      .flatten

}
