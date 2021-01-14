package core.generators

import java.time.Instant

import akka.util.ByteString
import core.{Block, Nonce, Transaction}
import org.scalacheck.Gen
import tests.BaseGenerators

import scala.util.Random

trait CoreGenerators {

  private val hexadecimalChar = "0123456789abcdef".toList

  @scala.annotation.tailrec
  private def genHexStr64(hex: List[Char] = Nil, count: Int = 64): List[Char] = {
    if (count < 1) hex
    else genHexStr64(hexadecimalChar(Random.nextInt(16)) :: hex, count - 1)
  }

  val genHex64Str: Gen[String] = Gen.const(genHexStr64().mkString)

  val genNonce: Gen[Nonce] = BaseGenerators.genPosInteger.map(Nonce(_))

  val genTransaction: Gen[Transaction] = for {
    sender   <- genHex64Str
    receiver <- genHex64Str
    amount   <- BaseGenerators.genFloatWithFractionalNotZero.map(BigDecimal(_))
    nonce    <- genNonce
    transactionFeeBasePrice <- BaseGenerators.genFloatWithFractionalNotZero.map(BigDecimal(_))
    transactionFeeLimit     <- BaseGenerators.genFloatWithFractionalNotZero.map(BigDecimal(_))
  } yield Transaction(sender, receiver, amount, nonce, transactionFeeBasePrice, transactionFeeLimit)

  val genBlock: Gen[Block] = for {
    prevHash     <- genHex64Str.map(ByteString(_).toArray)
    transactions <- Gen.listOf(genTransaction)
    millis       <- Gen.const(Instant.now.toEpochMilli)
  } yield Block.apply(prevHash, transactions, millis)

}
