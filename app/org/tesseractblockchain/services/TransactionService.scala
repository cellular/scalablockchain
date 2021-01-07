package org.tesseractblockchain.services

import cats.Applicative
import cats.effect._
import cats.implicits._
import core.exceptions.TransactionNotFoundException
import core.{BlockSize, Offset, Transaction}
import javax.inject.Inject
import org.tesseractblockchain.BlockchainEnvironment
import zio._
import zio.interop.catz.taskConcurrentInstance
import zio.stm.STM

private[tesseractblockchain] class TransactionService @Inject()() {

  def sendTransaction(tx: Transaction): ZIO[BlockchainEnvironment, Throwable, Unit] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        pendingTransactionsRef <- env.dependencyEnv.pendingTransactions
        pendingTransactions    <- pendingTransactionsRef.get
        _                      <- Applicative[Task].map2(
                                 STM.atomically(pendingTransactions.addPendingTransaction(tx)),
                                 env.dependencyEnv.miner0.>>=(_.update(_.map(_.copy(canceledBlock = true))))
                               )((ptx, _) => pendingTransactionsRef.set(ptx))
      } yield ()
    }

  def getTransactionByHash(hex: String): ZIO[BlockchainEnvironment, Throwable, Transaction] =
    ZIO
      .accessM[BlockchainEnvironment](_.dependencyEnv.blockchain.getInstance(_.getTransactionByHash(hex)))
      .flatten
      .mapError(_ => TransactionNotFoundException(hex))

  def getRecentTransactions(size: BlockSize, offset: Offset): ZIO[BlockchainEnvironment, Throwable, List[Transaction]] =
    ZIO
      .accessM[BlockchainEnvironment](
        _.dependencyEnv.blockchain.getInstance(_.getLatestBlocks(size, offset).map(_.>>=(_.transactions))))
      .flatten

}
