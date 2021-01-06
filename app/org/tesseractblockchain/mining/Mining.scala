package org.tesseractblockchain.mining

import java.time.Clock
import cats.effect._
import core.{Block, Miner}
import org.tesseractblockchain.DependencyManager
import zio.interop.catz.{core, monadErrorInstance, _}
import zio.{Fiber, _}

private[tesseractblockchain] object Mining {

  def mining: ZIO[Clock with DependencyManager, Throwable, Fiber.Runtime[Throwable, Ref[Miner]]] =
    for {
      block <- Mining.getNewBlockForMining
      miner <- Ref.make(Miner(block, isMining = true, canceledBlock = false))
      fiber <- mining(miner).fork
    } yield fiber

  // def registerMiners(miner: Miner): Task[Unit] = miners.flatMap(_.update(miner :: _))

  // private val miners: UIO[Ref[List[Miner]]] = Ref.make(Nil)

  def mining(minerRef: Ref[Miner]): ZIO[Clock with DependencyManager, Throwable, Ref[Miner]] =
    ZIO.accessM[Clock with DependencyManager] { env =>
      minerRef.get.flatMap { miner =>
        for {
          updatedMinerRef        <- incrementNonceOrElseRestartMining(miner.block, minerRef, miner.canceledBlock)
          canceledBlockPredicate  = ZIO.succeed(!miner.canceledBlock)
          prevBlockHash          <- miner.block.getBlockHash
          maybeMinedBlock        <- ZIO.ifM(canceledBlockPredicate)(
                                      blockMined(miner.block),
                                      Task.succeed(Block.apply(prevBlockHash, env.millis()))
                                    )
          maybeCanceledBlock     <- ZIO.ifM(canceledBlockPredicate)(Task.succeed(true), Task.succeed(false))
          _                      <- updatedMinerRef.update(_.copy(maybeMinedBlock, miner.isMining, maybeCanceledBlock))
          _                      <- ZIO.when(miner.isMining)(createNewBlock(updatedMinerRef).flatMap(mining))
        } yield updatedMinerRef
      }
    }

  def incrementNonceOrElseRestartMining(
    block: Block,
    minerRef: Ref[Miner],
    canceledBlock: Boolean
  ): ZIO[Clock with DependencyManager, Throwable, Ref[Miner]] =
    ZIO.accessM[Clock with DependencyManager] { env =>
      minerRef.get.flatMap { miner =>
        val fnRestartMining: Miner => ZIO[Clock with DependencyManager, Nothing, Unit] =
          Mining.restartMining(_).>>=(block => minerRef.update(m => Miner(block, m.isMining, m.canceledBlock)))

        for {
          hash                     <- block.getBlockHash
          blockchainRef            <- env.blockchain
          blockchain               <- blockchainRef.get
          doesNotFulfillDifficulty <- ZIO.succeed(blockchain.fulfillsDifficulty(hash))
          _                        <- ZIO.when(!canceledBlock && doesNotFulfillDifficulty)(
                                        block.incrementNonce.orElse(fnRestartMining(miner)) *>
                                        incrementNonceOrElseRestartMining(block, minerRef, canceledBlock))
        } yield minerRef
      }
    }

  def restartMining(miner: Miner): ZIO[Clock with DependencyManager, Nothing, Block] =
    ZIO.accessM[Clock with DependencyManager] { env =>
      (for {
        pendingTransactionsRef <- env.pendingTransactions
        pendingTransactions    <- pendingTransactionsRef.get
        nextBlockTxs           <- pendingTransactions.getTransactionsForNextBlock.commit
      } yield nextBlockTxs).map { transactions =>
        Block(miner.block.previousHash, transactions, env.millis())
      }
    }

  def createNewBlock(minerRef: Ref[Miner]): ZIO[Clock with DependencyManager, Throwable, Ref[Miner]] =
    Mining
      .getNewBlockForMining
      .map(block => minerRef.update(_.copy(block = block)))
      .map(_ => minerRef)

  def getNewBlockForMining: ZIO[Clock with DependencyManager, Throwable, Block] =
    ZIO.accessM[Clock with DependencyManager] { env =>
      for {
        blockchainRef          <- env.blockchain
        blockchain             <- blockchainRef.get
        prevHash               <- blockchain.getPreviousHash
        pendingTransactionsRef <- env.pendingTransactions
        pendingTransactions    <- pendingTransactionsRef.get
        transactions           <- pendingTransactions.getTransactionsForNextBlock.commit
      } yield Block(previousHash = prevHash, transactions = transactions, env.millis())
    }

  def blockMined(block: Block): ZIO[Clock with DependencyManager, Throwable, Block] =
    ZIO.accessM[Clock with DependencyManager] { env =>
      for {
        transactions           <- block.getBlockHash.map(hash => block.transactions.map(_.apply(hash)))
        updatedBlock            = block.copy(transactions = transactions)
        blockchainRef          <- env.blockchain
        blockchain             <- blockchainRef.get
        updatedBlockchain      <- blockchain.addBlock(updatedBlock)
        _                      <- blockchainRef.set(updatedBlockchain)
        pendingTransactionsRef <- env.pendingTransactions
        pendingTransactions    <- pendingTransactionsRef.get
        _                      <- pendingTransactions.clearPendingTransactions(updatedBlock).commit
        // _ <- notifyNewBlock(block) // TODO optional
      } yield updatedBlock
    }

  // TODO
  // private def notifyNewBlock(block: Block): Task[Unit] = {
  //   Task.succeed(())
  // }

}
