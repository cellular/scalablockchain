package org.tesseractblockchain.logic

import java.math.BigInteger
import java.time.Instant

import akka.util.ByteString
import core._
import core.exceptions.TransactionNotFoundException
import core.fixtures.CoreFixtures
import tests.TestSpec
import zio.{Ref, UIO, ZIO}

import scala.util.Try

class BlockchainSpec extends TestSpec {

  private val amount = BigDecimal(1432.1234567123456789)
  private val transactionFeeBasePrice = BigDecimal(0.00001)
  private val transactionFeeLimit = BigDecimal(0.01)
  private val millis = Instant.now.toEpochMilli
  private val transaction = Transaction(
    sender = CoreFixtures.hashStr2,
    receiver = CoreFixtures.hashStr2,
    amount = amount,
    nonce = Nonce(0),
    transactionFeeBasePrice = transactionFeeBasePrice,
    transactionFeeLimit = transactionFeeLimit
  )
  val previousHashGenesis = GenesisBlock.genesisBlock.previousHash
  val previousHash1 = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
  val previousHash2 = ByteString("148fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
  val previousHash3 = ByteString("248fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray

  "Blockchain#fulfillsDifficulty" must {
    val fnBlockchain: BigInteger => ZIO[Any, Nothing, Boolean] =
      (difficulty: BigInteger) =>
        Ref.make(Blockchain(
          blockCache = BlockCache(Nil),
          transactionCache = TransactionCache(Nil)
        )).flatMap(_.map(_.fulfillsDifficulty(difficulty.toByteArray)).get)

    List(
      (
        "a true for the matched difficulty value",
        new BigInteger("-57896000000000000000000000000000000000000000000000000000000000000000000000000"),
        true
      ),
      (
        "a true when found almost the same difficulty value",
        new BigInteger("-57896000000000000000000000000000000000000000000000000000000000000000000000001"),
        true
      ),
      (
        "a false for the unmatched difficulty value",
        new BigInteger("57896000000000000000000000000000000000000000000000000000000000000000000000001"),
        false
      ),
      (
        "a false for the almost matched difficulty value of",
        new BigInteger("0"),
        false
      ),
      (
        "a true for a valid value of the difficulty lesser than '0'",
        new BigInteger("-1"),
        false
      )
    ).foreach { case (label, difficulty, expected) =>
      s"return $label ${difficulty.toString}" in {
        testZIO(fnBlockchain(difficulty))(_ mustBe Right(expected))
      }
    }
  }

  "Blockchain#addBlock" must {
    "return the current blockchain after added block" in {
      val block1 = Block.apply(previousHash1, millis)
      val block2 = Block.apply(previousHash2, millis)
      val block3 = Block.apply(previousHash3, millis)
      val blockchainUioRef: UIO[Ref[Blockchain]] = Ref.make(Blockchain(
        blockCache       = BlockCache(Nil),
        transactionCache = TransactionCache(Nil)
      ))

      testZIO(for {
        blockchainRef <- blockchainUioRef
        // test
        blockchain0   <- blockchainRef.map(_.addBlock(block1)).get.flatten
        blockchain1   <- blockchain0.addBlock(block2)
        blockchain    <- blockchain1.addBlock(block3)
      } yield blockchain) {
        case Right(blockchain) =>
          blockchain.chain.blocks match {
            case Nil => fail("empty chain @addBlock")
            case blocks =>
              blocks.length           mustBe 4
              blocks.headOption       mustBe Some(block3)
              Try(blocks(1)).toOption mustBe Some(block2)
              Try(blocks(2)).toOption mustBe Some(block1)
              Try(blocks(3)).toOption mustBe Some(GenesisBlock.genesisBlock)
          }
          blockchain.blockCache.value.map(_._2).contains(block1) mustBe true
          blockchain.blockCache.value.map(_._2).contains(block2) mustBe true
          blockchain.blockCache.value.map(_._2).contains(block3) mustBe true
        case Left(_) => fail("addBlock")
      }
    }
  }

  "Blockchain#getInstance getBlockByHash" must {
    "return a block by hashStr via getInstance which applied a function: f: Blockchain => T" in {
      val block = Block.apply(previousHash1, millis)
      val blockchainUioRef: UIO[Ref[Blockchain]] = Ref.make(Blockchain(
        blockCache       = BlockCache((CoreFixtures.hashStr1,  block) :: Nil),
        transactionCache = TransactionCache((CoreFixtures.hashStr2, transaction) :: Nil)
      ))
      testZIO(for {
        blockchainRef <- blockchainUioRef
        bc            <- blockchainRef.map(_.addBlock(block)).get.flatten
        _             <- blockchainRef.set(bc)
        // test
        block0        <- blockchainUioRef.getInstance(_.getBlockByHash(CoreFixtures.hashStr1)).flatten
      } yield block0)(_ mustBe Right(block))
    }
  }

  "Blockchain#getPreviousHash" must {
    "return the prev hash copyWith latest block copyWith blockHeader of the prev block" in {
      testZIO(for {
        blockchainRef <- Ref.make(Blockchain(
                           blockCache       = BlockCache(Nil),
                           transactionCache = TransactionCache(Nil)
                         ))
        blockchain0   <- blockchainRef.map(_.addBlock(Block.apply(previousHash1, millis))).get.flatten
        blockchain    <- blockchain0.addBlock(Block.apply(previousHash2, millis))
        // test
        hash          <- blockchain.getPreviousHash
      } yield hash) {
        case Right(arr) => arr mustBe a[Array[_]]
        case _ => fail("getPreviousHash")
      }
    }
  }

  "Blockchain#getTransactionByHash" must {
    "return a found transaction" in {
      val transactionCache = TransactionCache((CoreFixtures.hashStr2, transaction) :: Nil)
      val blockchain = Blockchain(blockCache = BlockCache(Nil), transactionCache = transactionCache)

      testZIO(blockchain.getTransactionByHash(CoreFixtures.hashStr2))(_ mustBe Right(transaction))
    }
    "return a TransactionNotFoundException" in {
      val blockchain = Blockchain(blockCache = BlockCache(Nil), transactionCache = TransactionCache(Nil))

      testZIO(blockchain.getTransactionByHash(CoreFixtures.hashStr2))(
        _ mustBe Left(TransactionNotFoundException(CoreFixtures.hashStr2))
      )
    }
  }

  "Blockchain#getLatestBlock" must {
    "return the latest block" in {
      val block3 = Block.apply(previousHash3, millis)

      testZIO(for {
        blockchainRef <- Ref.make(Blockchain(
                           blockCache       = BlockCache(Nil),
                           transactionCache = TransactionCache(Nil)
                         ))
        blockchain0   <- blockchainRef.map(_.addBlock(Block.apply(previousHash1, millis))).get.flatten
        blockchain1   <- blockchain0.addBlock(Block.apply(previousHash2, millis))
        blockchain    <- blockchain1.addBlock(block3)
      } yield blockchain) {
        // test
        case Right(blockchain) => blockchain.chain.getLastBlock mustBe block3
        case Left(_) => fail("getLatestBlock")
      }
    }
  }

  "Blockchain#getLatestBlocks" must {
    "return the latest blocks" in {
      val blocks0 = for {
        block0Hash <- GenesisBlock.genesisBlock.getBlockHash
        block1      = Block.apply(block0Hash, millis)
        blockHash1 <- block1.getBlockHash
        block2      = Block.apply(blockHash1, millis)
        blockHash2 <- block2.getBlockHash
        block3      = Block.apply(blockHash2, millis)
      } yield (block1, block2, block3)

      val defaultBlockSize = BlockSize(10)
      val defaultOffset = Offset(0)
      val blockchainRef0 = Ref.make(Blockchain(blockCache = BlockCache(Nil), transactionCache = TransactionCache(Nil)))

      // only the genesis block inside the blockCache
      testZIO(blockchainRef0) {
        case Right(ref) =>
          testZIO(ref.get.map(_.chain.getLastBlock.previousHash)) {
            case Right(genesisHash) => genesisHash mustEqual previousHashGenesis
            case Left(_) => fail("first/last hash is not genesis")
          }
        case Left(_) => fail("unexpected: check initial block")
      }

      testZIO(for {
        blockchainRef <- blockchainRef0
        blocks        <- blocks0
        (block1, block2, block3) = blocks
        blockchain0   <- blockchainRef.map(_.addBlock(block1)).get.flatten
        blockchain1   <- blockchain0.addBlock(block2)
        blockchain    <- blockchain1.addBlock(block3)

        _              = blockchain.chain.getLastBlock mustBe block3  // self check
        cache          = blockchain.blockCache.value.map(_._2)
        _              = cache.contains(block3) mustBe true       // self check
        _              = cache.contains(block2) mustBe true       // self check
        _              = cache.contains(block1) mustBe true       // self check

        // test
        latest        <- blockchain.getLatestBlocks(defaultBlockSize, defaultOffset)
        _              = latest.contains(block3) mustBe true
        _              = latest.contains(block2) mustBe true
        _              = latest.contains(block1) mustBe true
      } yield ())(_ mustBe Right(()))
    }
    "return the latest blocks - BlockSize(1), Offset(1)" in {
      val blocks0 = for {
        block0Hash <- GenesisBlock.genesisBlock.getBlockHash
        block1      = Block.apply(block0Hash, millis)
        blockHash1 <- block1.getBlockHash
        block2      = Block.apply(blockHash1, millis)
        blockHash2 <- block2.getBlockHash
        block3      = Block.apply(blockHash2, millis)
        blockHash3 <- block3.getBlockHash
        block4      = Block.apply(blockHash3, millis)
        blockHash4 <- block4.getBlockHash
        block5      = Block.apply(blockHash4, millis)
      } yield (block1, block2, block3, block4, block5)

      val defaultBlockSize = BlockSize(1)
      val defaultOffset = Offset(1)
      val blockchainRef0 = Ref.make(Blockchain(blockCache = BlockCache(Nil), transactionCache = TransactionCache(Nil)))

      // only the genesis block inside the blockCache
      testZIO(blockchainRef0) {
        case Right(ref) =>
          testZIO(ref.get.map(_.chain.getLastBlock.previousHash)) {
            case Right(genesisHash) => genesisHash mustEqual previousHashGenesis
            case Left(_) => fail("first/last hash is not genesis")
          }
        case Left(_) => fail("unexpected: check initial block")
      }

      testZIO(for {
        blockchainRef <- blockchainRef0
        blocks        <- blocks0
        (block1, block2, block3, block4, block5) = blocks
        blockchain0   <- blockchainRef.map(_.addBlock(block1)).get.flatten
        blockchain1   <- blockchain0.addBlock(block2)
        blockchain2   <- blockchain1.addBlock(block3)
        blockchain3   <- blockchain2.addBlock(block4)
        blockchain    <- blockchain3.addBlock(block5)

        _              = blockchain.chain.getLastBlock mustBe block5  // self check
        cache          = blockchain.blockCache.value.map(_._2)
        _              = cache.contains(block5) mustBe true       // self check
        _              = cache.contains(block4) mustBe true       // self check
        _              = cache.contains(block3) mustBe true       // self check
        _              = cache.contains(block2) mustBe true       // self check
        _              = cache.contains(block1) mustBe true       // self check

        // test
        latest        <- blockchain.getLatestBlocks(defaultBlockSize, defaultOffset)
        _              = latest.contains(block5) mustBe true
        _              = latest.contains(block4) mustBe true
        _              = latest.contains(block3) mustBe false
        _              = latest.contains(block2) mustBe false
        _              = latest.contains(block1) mustBe false
      } yield ())(_ mustBe Right(()))
    }
    "return the latest blocks - BlockSize(2), Offset(1)" in {
      val blocks0 = for {
        block0Hash <- GenesisBlock.genesisBlock.getBlockHash
        block1      = Block.apply(block0Hash, millis)
        blockHash1 <- block1.getBlockHash
        block2      = Block.apply(blockHash1, millis)
        blockHash2 <- block2.getBlockHash
        block3      = Block.apply(blockHash2, millis)
        blockHash3 <- block3.getBlockHash
        block4      = Block.apply(blockHash3, millis)
        blockHash4 <- block4.getBlockHash
        block5      = Block.apply(blockHash4, millis)
      } yield (block1, block2, block3, block4, block5)

      val defaultBlockSize = BlockSize(2)
      val defaultOffset = Offset(1)
      val blockchainRef0 = Ref.make(Blockchain(blockCache = BlockCache(Nil), transactionCache = TransactionCache(Nil)))

      // only the genesis block inside the blockCache
      testZIO(blockchainRef0) {
        case Right(ref) =>
          testZIO(ref.get.map(_.chain.getLastBlock.previousHash)) {
            case Right(genesisHash) => genesisHash mustEqual previousHashGenesis
            case Left(_) => fail("first/last hash is not genesis")
          }
        case Left(_) => fail("unexpected: check initial block")
      }

      testZIO(for {
        blockchainRef <- blockchainRef0
        blocks        <- blocks0
        (block1, block2, block3, block4, block5) = blocks
        blockchain0   <- blockchainRef.map(_.addBlock(block1)).get.flatten
        blockchain1   <- blockchain0.addBlock(block2)
        blockchain2   <- blockchain1.addBlock(block3)
        blockchain3   <- blockchain2.addBlock(block4)
        blockchain    <- blockchain3.addBlock(block5)

        _              = blockchain.chain.getLastBlock mustBe block5  // self check
        cache          = blockchain.blockCache.value.map(_._2)
        _              = cache.contains(block5) mustBe true       // self check
        _              = cache.contains(block4) mustBe true       // self check
        _              = cache.contains(block3) mustBe true       // self check
        _              = cache.contains(block2) mustBe true       // self check
        _              = cache.contains(block1) mustBe true       // self check

        // test
        latest        <- blockchain.getLatestBlocks(defaultBlockSize, defaultOffset)
        _              = latest.contains(block5) mustBe true
        _              = latest.contains(block4) mustBe true
        _              = latest.contains(block3) mustBe true
        _              = latest.contains(block2) mustBe false
        _              = latest.contains(block1) mustBe false
      } yield ())(_ mustBe Right(()))
    }
    "return the latest blocks - BlockSize(3), Offset(2)" in {
      val blocks0 = for {
        block0Hash <- GenesisBlock.genesisBlock.getBlockHash
        block1      = Block.apply(block0Hash, millis)
        blockHash1 <- block1.getBlockHash
        block2      = Block.apply(blockHash1, millis)
        blockHash2 <- block2.getBlockHash
        block3      = Block.apply(blockHash2, millis)
        blockHash3 <- block3.getBlockHash
        block4      = Block.apply(blockHash3, millis)
        blockHash4 <- block4.getBlockHash
        block5      = Block.apply(blockHash4, millis)
      } yield (block1, block2, block3, block4, block5)

      val defaultBlockSize = BlockSize(3)
      val defaultOffset = Offset(2)
      val blockchainRef0 = Ref.make(Blockchain(blockCache = BlockCache(Nil), transactionCache = TransactionCache(Nil)))

      // only the genesis block inside the blockCache
      testZIO(blockchainRef0) {
        case Right(ref) =>
          testZIO(ref.get.map(_.chain.getLastBlock.previousHash)) {
            case Right(genesisHash) => genesisHash mustEqual previousHashGenesis
            case Left(_) => fail("first/last hash is not genesis")
          }
        case Left(_) => fail("unexpected: check initial block")
      }

      testZIO(for {
        blockchainRef <- blockchainRef0
        blocks        <- blocks0
        (block1, block2, block3, block4, block5) = blocks
        blockchain0   <- blockchainRef.map(_.addBlock(block1)).get.flatten
        blockchain1   <- blockchain0.addBlock(block2)
        blockchain2   <- blockchain1.addBlock(block3)
        blockchain3   <- blockchain2.addBlock(block4)
        blockchain    <- blockchain3.addBlock(block5)

        _              = blockchain.chain.getLastBlock mustBe block5  // self check
        cache          = blockchain.blockCache.value.map(_._2)
        _              = cache.contains(block5) mustBe true       // self check
        _              = cache.contains(block4) mustBe true       // self check
        _              = cache.contains(block3) mustBe true       // self check
        _              = cache.contains(block2) mustBe true       // self check
        _              = cache.contains(block1) mustBe true       // self check

        // test
        latest        <- blockchain.getLatestBlocks(defaultBlockSize, defaultOffset)
        _              = latest.contains(block5) mustBe true
        _              = latest.contains(block4) mustBe true
        _              = latest.contains(block3) mustBe true
        _              = latest.contains(block2) mustBe true
        _              = latest.contains(block1) mustBe false
      } yield ())(_ mustBe Right(()))
    }
    "return the latest blocks - BlockSize(2), Offset(3)" in {
      val blocks0 = for {
        block0Hash <- GenesisBlock.genesisBlock.getBlockHash
        block1      = Block.apply(block0Hash, millis)
        blockHash1 <- block1.getBlockHash
        block2      = Block.apply(blockHash1, millis)
        blockHash2 <- block2.getBlockHash
        block3      = Block.apply(blockHash2, millis)
        blockHash3 <- block3.getBlockHash
        block4      = Block.apply(blockHash3, millis)
        blockHash4 <- block4.getBlockHash
        block5      = Block.apply(blockHash4, millis)
      } yield (block1, block2, block3, block4, block5)

      val defaultBlockSize = BlockSize(2)
      val defaultOffset = Offset(3)
      val blockchainRef0 = Ref.make(Blockchain(blockCache = BlockCache(Nil), transactionCache = TransactionCache(Nil)))

      // only the genesis block inside the blockCache
      testZIO(blockchainRef0) {
        case Right(ref) =>
          testZIO(ref.get.map(_.chain.getLastBlock.previousHash)) {
            case Right(genesisHash) => genesisHash mustEqual previousHashGenesis
            case Left(_) => fail("first/last hash is not genesis")
          }
        case Left(_) => fail("unexpected: check initial block")
      }

      testZIO(for {
        blockchainRef <- blockchainRef0
        blocks        <- blocks0
        (block1, block2, block3, block4, block5) = blocks
        blockchain0   <- blockchainRef.map(_.addBlock(block1)).get.flatten
        blockchain1   <- blockchain0.addBlock(block2)
        blockchain2   <- blockchain1.addBlock(block3)
        blockchain3   <- blockchain2.addBlock(block4)
        blockchain    <- blockchain3.addBlock(block5)

        _              = blockchain.chain.getLastBlock mustBe block5  // self check
        cache          = blockchain.blockCache.value.map(_._2)
        _              = cache.contains(block5) mustBe true       // self check
        _              = cache.contains(block4) mustBe true       // self check
        _              = cache.contains(block3) mustBe true       // self check
        _              = cache.contains(block2) mustBe true       // self check
        _              = cache.contains(block1) mustBe true       // self check

        // test
        latest        <- blockchain.getLatestBlocks(defaultBlockSize, defaultOffset)
        _              = latest.contains(block5) mustBe true
        _              = latest.contains(block4) mustBe true
        _              = latest.contains(block3) mustBe true
        _              = latest.contains(block2) mustBe false
        _              = latest.contains(block1) mustBe false
      } yield ())(_ mustBe Right(()))
    }
    "return the latest blocks - only genesis block" in {
      val genesis = GenesisBlock.genesisBlock

      val defaultBlockSize = BlockSize(10)
      val defaultOffset = Offset(0)
      val blockchainRef0 = Ref.make(Blockchain(blockCache = BlockCache(Nil), transactionCache = TransactionCache(Nil)))

      testZIO(for {
        blockchainRef <- blockchainRef0
        blockchain    <- blockchainRef.get

        // test
        latest        <- blockchain.getLatestBlocks(defaultBlockSize, defaultOffset)
        _              = latest.contains(genesis) mustBe true
      } yield ())(_ mustBe Right(()))
    }
  }

  "Blockchain#getChildOfBlock" must {
    "return the current child block2 of block3" in {
      val blocks0 = for {
        block0Hash <- GenesisBlock.genesisBlock.getBlockHash
        block1      = Block.apply(block0Hash, millis)
        blockHash1 <- block1.getBlockHash
        block2      = Block.apply(blockHash1, millis)
        blockHash2 <- block2.getBlockHash
        block3      = Block.apply(blockHash2, millis)
      } yield (block1, block2, block3)

      testZIO(for {
        blockchainRef <- Ref.make(Blockchain(
          blockCache = BlockCache(Nil),
          transactionCache = TransactionCache(Nil)
        ))
        blocks        <- blocks0
        (block1, block2, block3) = blocks
        blockchain0   <- blockchainRef.map(_.addBlock(block1)).get.flatten
        blockchain1   <- blockchain0.addBlock(block2)
        blockchain    <- blockchain1.addBlock(block3)

        // test
        latest        <- blockchain.getChildOfBlock(block3)
        _              = latest mustBe block2
      } yield ())(_ mustBe Right(()))
    }
  }

}
