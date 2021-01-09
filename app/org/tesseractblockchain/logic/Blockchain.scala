package org.tesseractblockchain.logic

import java.math.BigInteger

import core.util.catz.MonadExtension
import cats.Applicative
import cats.effect._
import cats.implicits._
import core._
import core.exceptions._
import core.util.converters.SHA3Helper
import zio._
import zio.interop.catz.{taskConcurrentInstance, _}

import scala.util.Try

private[tesseractblockchain] final case class Blockchain(
    blockCache: BlockCache,
    transactionCache: TransactionCache,
    chain: Chain = Chain(NetworkId(1)),
) {
  val difficulty = new BigInteger("-57896000000000000000000000000000000000000000000000000000000000000000000000000")
}

private[tesseractblockchain] object Blockchain {

  implicit class RichBlockchainUIORef(blockchainUIORef: UIO[Ref[Blockchain]]) {
    def getInstance[T](f: Blockchain => T): ZIO[Any, Nothing, T] = blockchainUIORef.>>=(_.get.map(f))
  }

  implicit class RichBlockchain(blockchain: Blockchain) {

    // TODO less than "0" should not the difficulty production value
    def fulfillsDifficulty(hash: Array[Byte]): Boolean = new BigInteger(hash).compareTo(blockchain.difficulty) <= 0

    def addBlock(block: Block): Task[Blockchain] =
      Applicative[Task].map3(
        f0 = (block.getBlockHash >>= SHA3Helper.digestToHex).map(hex =>
               blockchain.blockCache.copy((hex, block) :: blockchain.blockCache.value)
             ),
        f1 = block.transactions.flatTraverse(tx => tx.txIdAsHex.map((_, tx) :: blockchain.transactionCache.value)),
        f2 = Task.succeed(blockchain.chain.copy(blocks = block :: blockchain.chain.blocks))
      )((blockCache, txCacheTuples, chain) => Blockchain(blockCache, TransactionCache(txCacheTuples), chain))

    def getPreviousHash: Task[Array[Byte]] = blockchain.chain.getLastBlock.getBlockHash

    def size: Int = blockchain.chain.size

    def getTransactionByHash(hex: String): Task[Transaction] = blockchain.transactionCache.get(hex)

    def getBlockByHash(hex: String): Task[Block] = blockchain.blockCache.get(hex)

    def getBlockByHash(hash: Array[Byte]): Task[Block] = SHA3Helper.digestToHex(hash) >>= blockchain.blockCache.get

    def getLatestBlock: Block = blockchain.chain.getLastBlock

    def getChildOfBlock(block: Block): Task[Block] =
      Task.fromTry(Try(blockchain.chain.blocks.indexOf(block) + 1)).flatMap { index =>
        MonadExtension.foldOptionM(
          fa = Task.succeed(blockchain.chain.get(index)),
          fb = Task.fail(BlockChildNotFoundException(block))
        )
      }

    /**
     * first block is always the genesis block
     */
    def getLatestBlocks(size: BlockSize, offset: Offset): Task[List[Block]] = {
      def getLatestBlocks(blocks: List[Block], sizePlusOffset: Int): Task[List[Block]] =
        MonadExtension.foldOptionM[Task, List[Block]](
          fa = getBlockByHash(blocks.head.blockHeader.previousHash)
            .either
            .map(_.fold(_ => none[Block], _.some))
            .flatMap(_.traverse(block =>
              ZIO.ifM(Task.succeed(sizePlusOffset >= offset.value))(
                Task.succeed(block :: blocks),
                Task.succeed(blocks)
              ))),
          fb = Task.succeed(blocks)
        )

      Task(getLatestBlock).flatMap { block =>
        (0 until (size.value + offset.value)).foldLeft(Task(List(block))) {
          (blocksTask, sizePlusOffset) => blocksTask.flatMap(getLatestBlocks(_, sizePlusOffset))
        }
      }
    }
  }

}
