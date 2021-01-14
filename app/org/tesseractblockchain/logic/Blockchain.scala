package org.tesseractblockchain.logic

import java.math.BigInteger

import cats.effect._
import cats.implicits._
import cats.{Applicative, Id, Monad}
import core._
import core.exceptions._
import core.util.categorytheory.ControlMonad
import core.util.converters.SHA3Helper
import zio._
import zio.interop.catz.{taskConcurrentInstance, _}

import scala.util.Try

private[tesseractblockchain] final case class Blockchain(
    blockCache: BlockCache,
    transactionCache: TransactionCache,
    chain: Chain = Chain(NetworkId(1)),
) {
  // TODO must be a blockheader property
  val difficulty = new BigInteger("-57896000000000000000000000000000000000000000000000000000000000000000000000000")
}

private[tesseractblockchain] object Blockchain {

  implicit class RichBlockchainRef(blockchainUIORef: Ref[Blockchain]) {
    def getInstance[T](f: Blockchain => Task[T]): ZIO[Any, Nothing, T] = blockchainUIORef.get.flatMap(f)
  }

  implicit class RichBlockchain(blockchain: Blockchain) {

    // TODO less than "0" should not the difficulty production value
    def fulfillsDifficulty(hash: Array[Byte]): Boolean = new BigInteger(hash).compareTo(blockchain.difficulty) <= 0

    def addBlock(block: Block): Task[Blockchain] =
      Applicative[Task].map3(
        f0 = (block.getBlockHash >>= SHA3Helper.digestToHex).map(blockchain.blockCache.copyWith(_, block)),
        f1 =
          block.transactions
            .flatTraverse(tx => tx.txIdAsHex.map((_, tx) :: blockchain.transactionCache.value))
            .map(TransactionCache(_)),
        f2 = Task.succeed(blockchain.chain.copyWith(block))
      )(Blockchain.apply)

    def getPreviousHash: Task[Array[Byte]] = blockchain.chain.getLastBlock.getBlockHash

    def getTransactionByHash(hex: String): Task[Transaction] = blockchain.transactionCache.get(hex)

    def getBlockByHash(hex: String): Task[Block] = blockchain.blockCache.get(hex)

    def getChildOfBlock(block: Block): Task[Block] =
      Task.fromTry(Try(blockchain.chain.blocks.indexOf(block) + 1)).flatMap { index =>
        ControlMonad.foldOptionM(
          onFailure = Task.fail(BlockChildNotFoundException(block)),
          onSuccess = Task.succeed(blockchain.chain.get(index))
        )
      }

    def getLatestBlocks(size: BlockSize, offset: Offset): Task[List[Block]] = {
      def getLatestBlocks(blocks: List[Block], sizePlusOffset: Int): Task[List[Block]] =
        ControlMonad.foldOptionM[Task, List[Block]](
          onFailure = Task.succeed(blocks),
          // first block is always the genesis block, "blocks.head" is always existing
          onSuccess = (SHA3Helper.digestToHex(blocks.head.blockHeader.previousHash) >>= blockchain.blockCache.get)
            .either
            .map(_.toOption)
            .flatMap(_.traverse(block =>
              Monad[Id].ifM(sizePlusOffset >= offset.value)(block :: blocks, blocks).pure[Task]
            ))
        )

      Task(blockchain.chain.getLastBlock).flatMap { block =>
        (0 until (size.value + offset.value)).foldLeft(Task(List(block))) {
          (blocksTask, sizePlusOffset) => blocksTask.flatMap(getLatestBlocks(_, sizePlusOffset))
        }
      }
    }

  }

}
