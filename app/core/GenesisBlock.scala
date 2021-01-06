package core

import java.time.Instant

object GenesisBlock {

  private final val zeroHashIn = new Array[Byte](32)

  final val genesisBlock: Block = Block(previousHash = zeroHashIn, Instant.now.toEpochMilli)

}
