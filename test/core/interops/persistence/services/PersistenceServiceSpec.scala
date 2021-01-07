package core.interops.persistence.services

import java.time.Instant

import core.persistence.services.{FileReadChainService, FileWriteChainService}
import core.{Block, Chain, GenesisBlock, NetworkId}
import tests.TestSpec
import zio.Task
import zio.interop.catz._

class PersistenceServiceSpec extends TestSpec {

  private val fileReadChainService = mock[FileReadChainService]
  private val fileWriteChainService = mock[FileWriteChainService]

  private val millis = Instant.now.toEpochMilli
  private val baseRoot = s"${System.getProperty("user.home")}/tesseractblockchain/resources/chain"

  private val chain = for {
    block0Hash <- GenesisBlock.genesisBlock.getBlockHash
    block1      = Block.apply(block0Hash, millis)
    blockHash1 <- block1.getBlockHash
    block2      = Block.apply(blockHash1, millis)
    blockHash2 <- block2.getBlockHash
    block3      = Block.apply(blockHash2, millis)
    chain       = Chain(NetworkId(1), block1 :: block2 :: block3 :: Nil)
  } yield chain

  private val service = new PersistenceService(fileReadChainService, fileWriteChainService)

  "PersistenceService#readChain" must {
    "return a chain by persisted blocks" in {
      fileReadChainService.readChain(NetworkId(1), baseRoot) returns chain

      val program = for {
        chain    <- chain
        expected <- service.readChain(NetworkId(1))
      } yield (chain, expected)

      whenReady(program) {
        case Right((chain, expected)) =>
          chain.networkId mustBe expected.networkId
          chain.blocks.length mustBe expected.blocks.length
        case Left(_) => fail("unexpected error: readChain")
      }
    }
  }

  "PersistenceService#writeChain" must {
    "returns a Unit on success" in {
      val chain = Chain(NetworkId(1), Nil)
      fileWriteChainService.writeChain(chain, baseRoot) returns Task.unit
      whenReady(service.writeChain(chain))(_ mustBe Right(()))
    }
  }

}
