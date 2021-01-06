package core

import play.api.libs.json.{JsNumber, JsObject, JsString, Json}
import tests.TestSpec

class TransactionSpec extends TestSpec {

  private trait TestSetup {
    val sender =
      "048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb"
    val receiver =
      "057fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb"
    val amount = BigDecimal(1432.1234567123456789)
    val transactionFeeBasePrice = BigDecimal(0.00001)
    val transactionFeeLimit = BigDecimal(0.01)
  }

  "Transaction#txId" must {
    "returns the txId of the current Transaction instance" in new TestSetup {
      val tx = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      val txHash = tx.txId
      whenReady(tx.txId) {
        case Right(hash0) =>
          whenReady(txHash) {
            case Right(hash1) => hash0 mustEqual hash1
            case _ => fail()
          }
        case _ => fail()
      }
    }
  }

  "Transaction#txIdAsHex" must {
    "returns the txIdAsHex of the current Transaction instance" in new TestSetup {
      val tx = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )

      val program = for {
        hash1 <- tx.txIdAsHex
        hash2 <- tx.txIdAsHex
      } yield hash1 == hash2

      whenReady(program)(_ mustBe Right(true))
    }
  }

  "Transaction#createTransactionHashCode" must {
    "returns an Integer of HashCode" in new TestSetup {
      val tx = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      whenReady(tx.hashCodeTask)(_.isRight mustBe true)
    }
  }

  "Transaction#equals" must {
    "returns a false when the second tx is not provided" in new TestSetup {
      val tx1 = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      whenReady(tx1.equals(None))(_ mustBe Right(false))
    }
    "returns a true when the transactions are equal" in new TestSetup {
      val tx1 = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      val tx2 = tx1
      whenReady(tx1.equals(Some(tx2)))(_ mustBe Right(true))
    }
    "returns a false when the transactions are not equal - two difference addresses" in new TestSetup {
      val tx1 = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      val tx2 = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      whenReady(tx1.equals(Some(tx2)))(_ mustBe Right(false))
    }
  }

  "Transaction#writes" must {
    "returns a fully json instance" in new TestSetup {
      val tx = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      Json.toJson(tx) mustBe JsObject(List(
        "sender" -> JsString(sender),
        "receiver" -> JsString(receiver),
        "amount" -> JsNumber(amount),
        "nonce" -> JsNumber(0),
        "transactionFeeBasePrice" -> JsNumber(transactionFeeBasePrice),
        "transactionFeeLimit" -> JsNumber(transactionFeeLimit)
      ))
    }
  }

  "Transaction#reads" must {
    "returns reads a fully instance from json" in new TestSetup {
      val json: String =
        s"""
          |{
          |  "sender": "$sender",
          |  "receiver": "$receiver",
          |  "amount": $amount,
          |  "nonce": 0,
          |  "transactionFeeBasePrice": $transactionFeeBasePrice,
          |  "transactionFeeLimit": $transactionFeeLimit
          |}
          |""".stripMargin
      val Some(tx) = Json.parse(json).validate[Transaction].asOpt
      tx.sender mustBe sender.getBytes("UTF-8")
      tx.receiver mustBe receiver.getBytes("UTF-8")
      tx.amount mustBe amount
      tx.nonce.value mustBe 0
      tx.transactionFeeBasePrice mustBe transactionFeeBasePrice
      tx.transactionFeeLimit mustBe transactionFeeLimit
    }
  }

  "Transaction#ordering" must {
    "returns the transaction with higher transactionFeeBasePrice and transactionFeeLimit" in new TestSetup {
      val factor = 100
      val tx1 = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice,
        transactionFeeLimit = transactionFeeLimit
      )
      val tx2 = Transaction.apply(
        sender = sender,
        receiver = receiver,
        amount = amount * factor,
        nonce = Nonce(0),
        transactionFeeBasePrice = transactionFeeBasePrice * factor,
        transactionFeeLimit = transactionFeeLimit * factor
      )

      Array(tx1, tx2).sorted(Transaction.ordering).toList match {
        case Nil => fail()
        case h :: _ => h mustBe tx2
      }
    }
  }

}
