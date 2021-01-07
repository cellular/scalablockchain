package org.tesseractblockchain

import cats.effect._
import core.{BlockCache, Miner, TransactionCache}
import org.tesseractblockchain.logic.{Blockchain, PendingTransactions}
import org.tesseractblockchain.mining.Mining
import zio.interop.catz._
import zio.{Fiber, Ref, UIO, ZIO}

private[tesseractblockchain] trait DependencyManager {
  val pendingTransactions: UIO[Ref[PendingTransactions]] = Ref.make(PendingTransactions.apply)
  val blockchain: UIO[Ref[Blockchain]] = Ref.make(Blockchain(BlockCache(Nil), TransactionCache(Nil)))
  val miner0: UIO[Ref[Option[Miner]]] = Ref.make(None)

  def runMining(miner0: UIO[Ref[Option[Miner]]] = miner0):
  ZIO[BlockchainEnvironment, Throwable, Fiber.Runtime[Throwable, Ref[Miner]]] = {
    (for {
      fiber     <- Mining.mining
      minerRef  <- fiber.join
      miner     <- minerRef.get
      miner0Ref <- miner0
      _         <- miner0Ref.set(Some(miner))
    } yield runMining(miner0)).flatten
  }

}
