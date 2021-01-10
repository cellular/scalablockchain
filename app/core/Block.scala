
package core

import cats.effect._
import cats.implicits._
import core.MagicNumber.MagicNumber
import core.util.SizeHelper
import core.util.categorytheory.ZioComonad._
import core.util.converters.SHA3Helper
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, Writes, __}
import zio.interop.catz.{core, _}
import zio.{Task, ZIO}

final case class Block(
    transactionCount: TransactionCount,
    transactions: List[Transaction],
    previousHash: Array[Byte],
    blockHeader: BlockHeader
) {
  val blockSize: BlockSize = BlockSize(SizeHelper.calculateBlockSize(this))
  @transient val magicNumber: MagicNumber = MagicNumber.magicNumber
}

object Block {

  implicit def writes: Writes[Block] = (
    (__ \ "transactionCount").write[TransactionCount] ~
    (__ \ "blockSize").write[BlockSize] ~
    (__ \ "previousHash").write[Array[Byte]] ~
    (__ \ "transactions").write[List[Transaction]] ~
    (__ \ "blockHeader").write[BlockHeader]
  ) (o => (o.transactionCount, o.blockSize, o.previousHash, o.transactions, o.blockHeader))

  implicit def reads: Reads[Block] = (
    (__ \ "transactionCount").read[TransactionCount] ~
    (__ \ "blockHeader").read[BlockHeader] ~
    (__ \ "previousHash").read[Array[Byte]] ~
    (__ \ "transactions").read[List[Transaction]]
  ) ((transactionCount, blockHeader, previousHash, transactions) => Block(
    transactionCount = transactionCount,
    blockHeader = blockHeader,
    previousHash = previousHash,
    transactions = transactions
  ))

  def getTransactionHash(transactions: List[Transaction]): Task[Array[Byte]] =
    transactions
      .traverse(_.txId).map(_.foldLeft(new Array[Byte](0))(_ ++ _))
      .>>=(SHA3Helper.hash256)

  implicit class RichBlock(block: Block) {
    def getBlockHash: Task[Array[Byte]] = block.blockHeader.asHash

    def incrementNonce: ZIO[Any, Throwable, Nonce] = block.blockHeader.incrementNonce

    def getTransactionHash: ZIO[Any, Throwable, Array[Byte]] =
      Block.getTransactionHash(block.transactions)
  }

  def apply(previousHash: Array[Byte], transactions: List[Transaction], timestamp: Long): Block = {
    Block(
      transactions = transactions,
      transactionCount = TransactionCount(transactions.length),
      previousHash = previousHash,
      blockHeader = createBlockHeader(previousHash, transactions, timestamp).extract
    )
  }

  def apply(previousHash: Array[Byte], timestamp: Long): Block =
    Block(
      transactions = Nil,
      transactionCount = TransactionCount(0),
      previousHash = previousHash,
      blockHeader = createBlockHeader(previousHash, Nil, timestamp).extract
    )

  private def createBlockHeader(
    previousHash: Array[Byte],
    transactions: List[Transaction],
    timestamp: Long
  ): Task[BlockHeader] =
    getTransactionHash(transactions).map(BlockHeader(timestamp, previousHash, _))

}
