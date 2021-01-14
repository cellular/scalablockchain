package core

import java.time.Instant

import akka.util.ByteString
import cats.Applicative
import cats.implicits._
import core.fixtures._
import core.util.converters.SHA3Helper
import play.api.libs.json.{JsSuccess, Json}
import tests.TestSpec
import zio.Task
import zio.interop.catz._

class BlockSpec extends TestSpec {

  private val millis = Instant.now.toEpochMilli

  "Block#apply(previousHash)" must {
    "return a instance of Block" in {
      val previousHash = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
      val block = Block(previousHash = previousHash, timestamp = millis)
      block.transactions mustBe Nil
      block.transactionCount mustBe TransactionCount(0)
      block.previousHash mustBe previousHash
    }
  }

  "Block#apply(previousHash, transactions)" must {
    "return a instance of Block" in {
      val previousHash = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
      val txs = CoreFixtures.transaction1 :: CoreFixtures.transaction2 :: Nil
      val block = Block(previousHash = previousHash, transactions = txs, timestamp = millis)
      block.transactions mustBe txs
      block.transactionCount mustBe TransactionCount(2)
      block.previousHash mustBe previousHash
    }
  }

  "Block#incrementNonce" must {
    "return an incremented Nonce" in {
      val previousHash = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
      val block = Block(previousHash, timestamp = millis)
      testZIO(block.incrementNonce)(_ mustBe Right(Nonce(1)))
    }
  }

  "Block#getTransactionHash" must {
    "return the hash of all transaction (txIds)" in {
      val previousHash = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
      val txs = CoreFixtures.transaction1 :: CoreFixtures.transaction2 :: Nil
      val txsHashesTask = txs.traverse(_.txId)
      val block = Block(previousHash = previousHash, transactions = txs, timestamp = millis)

      val program = for {
        hash     <- block.getTransactionHash
        expected <- txsHashesTask.map(_.foldLeft(new Array[Byte](0))(_ ++ _)).flatMap(SHA3Helper.hash256)
      } yield hash -> expected

      testZIO(program) {
        case Right((hash, expected)) => hash mustEqual expected
        case Left(_) => fail("unexpected error: getTransactionHash")
      }
    }
  }

  "Block#getBlockHash" must {
    "return a hash copyWith blockHeader which is unique in its timestamp" in {
      val previousHash = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
      val block = Block(previousHash, timestamp = millis)
      val program = Applicative[Task].tuple2(block.getBlockHash, block.blockHeader.asHash)
      testZIO(program) {
        case Right((hash, expected)) => hash must not be expected
        case Left(_) => fail("unexpected error: getBlockHash")
      }
    }
  }

  "Block#json" must {
    "return a json string copyWith a block instance" in {
      val previousHash = ByteString("048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb").toArray
      val block = Block(previousHash, Nil, timestamp = millis)
      Json.toJson(block).validate[Block] match {
        case JsSuccess(b, _) =>
          b.previousHash mustBe block.previousHash
          b.transactions mustBe block.transactions
        case _ => fail("json reads writes")
      }
    }
  }

}
