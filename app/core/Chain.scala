package core

import cats.effect._
import play.api.libs.json._
import zio.interop.catz._

final case class Chain(networkId: NetworkId, blocks: List[Block])

object Chain {

  private val genesis = GenesisBlock.genesisBlock

  implicit val writes: Writes[Chain] = Json.writes[Chain]

  def apply(networkId: NetworkId): Chain = Chain(networkId, genesis :: Nil)

  implicit class RichChain(chain: Chain) {
    def addBlock(block: Block): List[Block] = block :: chain.blocks

    def get(index: Int): Option[Block] = chain.blocks.lift(index)

    def getLastBlock: Block = chain.blocks.headOption.getOrElse(genesis)

    def size: Int = chain.blocks.length

    def copyWith(block: Block): Chain = chain.copy(blocks = block :: chain.blocks)
  }

}
