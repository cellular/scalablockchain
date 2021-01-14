package org.tesseractblockchain.mining

import cats.effect._
import core.{Block, Miner}
import org.tesseractblockchain.BlockchainEnvironment
import zio.interop.catz.{core, monadErrorInstance, _}
import zio.{Fiber, _}

private[tesseractblockchain] object Mining {

  def mining: ZIO[BlockchainEnvironment, Throwable, Fiber.Runtime[Throwable, Ref[Miner]]] =
    for {
      block <- Mining.getNewBlockForMining
      miner <- Ref.make(Miner(block, isMining = true, canceledBlock = false))
      fiber <- mining(miner).fork
    } yield fiber

  // def registerMiners(miner: Miner): Task[Unit] = miners.flatMap(_.update(miner :: _))

  // private val miners: UIO[Ref[List[Miner]]] = Ref.make(Nil)

  def mining(minerRef: Ref[Miner]): ZIO[BlockchainEnvironment, Throwable, Ref[Miner]] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      minerRef.get.flatMap { miner =>
        for {
          updatedMinerRef        <- incrementNonceOrElseRestartMining(miner.block, minerRef, miner.canceledBlock)
          canceledBlockPredicate  = ZIO.succeed(!miner.canceledBlock)
          prevBlockHash          <- miner.block.getBlockHash
          maybeMinedBlock        <- ZIO.ifM(canceledBlockPredicate)(
                                      blockMined(miner.block),
                                      Task.succeed(Block.apply(prevBlockHash, env.clockEnv.millis()))
                                    )
          maybeCanceledBlock     <- ZIO.ifM(canceledBlockPredicate)(Task.succeed(true), Task.succeed(false))
          _                      <- updatedMinerRef.update(_.copy(maybeMinedBlock, miner.isMining, maybeCanceledBlock))
          // TODO persist chain
          _                      <- ZIO.when(miner.isMining)(createNewBlock(updatedMinerRef).flatMap(mining))
        } yield updatedMinerRef
      }
    }

  def incrementNonceOrElseRestartMining(
    block: Block,
    minerRef: Ref[Miner],
    canceledBlock: Boolean
  ): ZIO[BlockchainEnvironment, Throwable, Ref[Miner]] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      minerRef.get.flatMap { miner =>
        val fnRestartMining: Miner => ZIO[BlockchainEnvironment, Nothing, Unit] =
          Mining.restartMining(_).>>=(block => minerRef.update(m => Miner(block, m.isMining, m.canceledBlock)))

        for {
          hash                     <- block.getBlockHash
          blockchainRef            <- env.dependencyEnv.blockchain
          blockchain               <- blockchainRef.get
          doesNotFulfillDifficulty <- ZIO.succeed(blockchain.fulfillsDifficulty(hash))
          _                        <- ZIO.when(!canceledBlock && doesNotFulfillDifficulty)(
                                        block.incrementNonce.orElse(fnRestartMining(miner)) *>
                                        incrementNonceOrElseRestartMining(block, minerRef, canceledBlock))
        } yield minerRef
      }
    }

  def restartMining(miner: Miner): ZIO[BlockchainEnvironment, Nothing, Block] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      (for {
        pendingTransactionsRef <- env.dependencyEnv.pendingTransactions
        pendingTransactions    <- pendingTransactionsRef.get
        nextBlockTxs           <- pendingTransactions.getTransactionsForNextBlock.commit
      } yield nextBlockTxs).map { transactions =>
        Block(miner.block.previousHash, transactions, env.clockEnv.millis())
      }
    }

  def createNewBlock(minerRef: Ref[Miner]): ZIO[BlockchainEnvironment, Throwable, Ref[Miner]] =
    Mining
      .getNewBlockForMining
      .map(block => minerRef.update(_.copy(block = block)))
      .map(_ => minerRef)

  def getNewBlockForMining: ZIO[BlockchainEnvironment, Throwable, Block] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        blockchainRef          <- env.dependencyEnv.blockchain
        blockchain             <- blockchainRef.get
        prevHash               <- blockchain.getPreviousHash
        pendingTransactionsRef <- env.dependencyEnv.pendingTransactions
        pendingTransactions    <- pendingTransactionsRef.get
        transactions           <- pendingTransactions.getTransactionsForNextBlock.commit
      } yield Block(previousHash = prevHash, transactions = transactions, env.clockEnv.millis())
    }

  def blockMined(block: Block): ZIO[BlockchainEnvironment, Throwable, Block] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        transactions           <- block.getBlockHash.map(hash => block.transactions.map(_.apply(hash)))
        updatedBlock            = block.copy(transactions = transactions)
        blockchainRef          <- env.dependencyEnv.blockchain
        blockchain             <- blockchainRef.get
        updatedBlockchain      <- blockchain.addBlock(updatedBlock)
        _                      <- blockchainRef.set(updatedBlockchain)
        pendingTransactionsRef <- env.dependencyEnv.pendingTransactions
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
