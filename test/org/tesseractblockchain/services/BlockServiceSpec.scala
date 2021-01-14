package org.tesseractblockchain.services

import java.time.{Clock, Instant, ZoneId}

import akka.util.ByteString
import core.fixtures.CoreFixtures
import core.{Block, BlockCache, TransactionCache, ZIORuntime}
import core.interops.persistence.services.PersistenceService
import org.tesseractblockchain.logic.Blockchain
import org.tesseractblockchain.{BlockchainEnvironment, DependencyManager}
import tests.TestSpec
import zio.{Ref, ZIO}

class BlockServiceSpec extends TestSpec {

  private trait TestSetup {
    val clock = mock[Clock]
    val persistenceService = mock[PersistenceService]
    val dependencyManager = mock[DependencyManager]

    val environment: BlockchainEnvironment = new BlockchainEnvironment {
      override val clockEnv: Clock = clock
      override val persistenceEnv: PersistenceService = persistenceService
      override val dependencyEnv: DependencyManager = dependencyManager
    }

    val previousHashBytes = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
    val service = new BlockService
  }

  "BlockService#getBlockByHash" must {
    "return a block by hash" in new TestSetup {
      val block: Block = Block.apply(previousHashBytes, Instant.now.toEpochMilli)

      environment.dependencyEnv.blockchain returns Ref.make(Blockchain(
        blockCache       = BlockCache((CoreFixtures.hashStr1,  block) :: Nil),
        transactionCache = TransactionCache(Nil)
      ))

      val preProgram: ZIO[Any, Throwable, Blockchain] = for {
        blockchainRef <- environment.dependencyEnv.blockchain
        blockchain0   <- blockchainRef.map(_.addBlock(block)).get.flatten
      } yield blockchain0

      testZIO(preProgram *> service.getBlockByHash(CoreFixtures.hashStr1).provide(environment))(
        _ mustBe Right(block)
      )
    }
  }

}
