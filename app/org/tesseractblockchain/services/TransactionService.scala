package org.tesseractblockchain.services

import cats.Applicative
import cats.effect._
import cats.implicits._
import core.exceptions.TransactionNotFoundException
import core.{BlockSize, Offset, Transaction}
import org.tesseractblockchain.BlockchainEnvironment
import zio._
import zio.interop.catz.taskConcurrentInstance
import zio.stm.STM

private[tesseractblockchain] class TransactionService {

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
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        blockchainRef <- env.dependencyEnv.blockchain
        transaction   <- blockchainRef.>>=(_.getTransactionByHash(hex))
      } yield transaction
    }.mapError(_ => TransactionNotFoundException(hex))

  def getRecentTransactions(
    size: BlockSize,
    offset: Offset
  ): ZIO[BlockchainEnvironment, Throwable, List[Transaction]] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        blockchainRef <- env.dependencyEnv.blockchain
        transactions  <- blockchainRef.>>=(_.getLatestBlocks(size, offset).map(_.>>=(_.transactions)))
      } yield transactions
    }

}
