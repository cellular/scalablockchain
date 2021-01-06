package core.interops.persistence.services

import java.io.File
import java.time.Instant

import core.persistence.services.FileWriteChainService
import core.{Block, Chain, GenesisBlock, NetworkId}
import tests.TestSpec
import zio.Task

class FileWriteChainServiceSpec extends TestSpec {

  private val millis = Instant.now.toEpochMilli
  private val path = "/core/persistence/chain/"

  "FileWriteChainService#readChain" must {
    "" in {
      val blocks = for {
        block0Hash <- GenesisBlock.genesisBlock.getBlockHash
        block1      = Block.apply(block0Hash, millis)
        blockHash1 <- block1.getBlockHash
        block2      = Block.apply(blockHash1, millis)
        blockHash2 <- block2.getBlockHash
        block3      = Block.apply(blockHash2, millis)
      } yield List(block1, block2, block3)

      val service = new FileWriteChainService

      val program = for {
        chain <- blocks.map(Chain(NetworkId(1), _))
        _     <- service.writeChain(chain, path)
      } yield ()

      whenReady(program)(_ mustBe Right(()))
    }
  }

}
