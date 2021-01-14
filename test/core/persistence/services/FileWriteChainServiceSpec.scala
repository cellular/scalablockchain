package core.persistence.services

import java.nio.file.Paths
import java.time.Instant

import core.{Block, Chain, GenesisBlock, NetworkId}
import tests.TestSpec

class FileWriteChainServiceSpec extends TestSpec {

  private val millis = Instant.now.toEpochMilli

  "FileWriteChainService#writeChain" must {
    "write new blocks as json into specified path" in {
      val path = s"${System.getProperty("user.dir")}/test/resources/core/persistence/chain"
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
        _     <- service.writeChain(chain = chain, path = Paths.get(path).toString)
      } yield ()

      testZIO(program)(_ mustBe Right(()))
    }
  }

}
