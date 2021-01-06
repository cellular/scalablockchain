package core.interops.persistence.services

import java.nio.file.Paths
import java.time.Instant

import core.persistence.services.FileWriteChainService
import core.{Block, Chain, GenesisBlock, NetworkId}
import play.api.Play
import tests.TestSpec

class FileWriteChainServiceSpec extends TestSpec {

  private val millis = Instant.now.toEpochMilli
  private val path = Paths.get(getClass.getResource("/core/persistence/chain/1").getPath)

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
        _     <- service.writeChain(chain = chain, path = path.toAbsolutePath.toString)
      } yield ()

      whenReady(program)(_ mustBe Right(()))
    }
  }

}
