package core

import cats.implicits._
import core.util.catz.ZioComonad.comonadTask
import core.util.converters.SHA3Helper
import java.nio.charset.StandardCharsets
import java.util.Objects
import cats.effect._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, Writes, __}
import zio.Task
import zio.interop.catz._

final class Transaction(
    val sender: Array[Byte],
    val receiver: Array[Byte],
    val amount: BigDecimal,
    val nonce: Nonce,
    val transactionFeeBasePrice: BigDecimal,
    val transactionFeeLimit: BigDecimal,
    @transient val blockId: Array[Byte] = Array.empty[Byte]
) extends Serializable {

  @transient val equals: Option[Transaction] => Task[Boolean] = {
    case None => Task.succeed(false)
    case Some(tx) if this == tx => Task.succeed(true)
    case Some(tx) =>
      for {
        transactionTxId <- this.txId
        furtherTxId     <- tx.txId
        equalsBasePrice  = this.transactionFeeBasePrice.equals(zero) && tx.transactionFeeBasePrice.equals(zero)
        equalsFeeLimit   = this.transactionFeeLimit.equals(zero) && tx.transactionFeeLimit.equals(zero)
        tTxID            = transactionTxId sameElements furtherTxId
        sender           = this.sender sameElements tx.sender
        amount           = this.amount.equals(zero) && tx.amount.equals(zero)
        receiver         = this.receiver sameElements tx.receiver
      } yield equalsBasePrice && equalsFeeLimit && tTxID && sender && amount && receiver
  }

  @transient def txId: Task[Array[Byte]] = SHA3Helper.hash256From(this)

  @transient def txIdAsHex: Task[String] = SHA3Helper.hash256AsHex(this)

  @transient def hashCodeTask: Task[Int] =
    for {
      txId <- this.txId
      r0   <- Task(Objects.hash(
        this.amount.doubleValue,
        this.nonce.value,
        this.transactionFeeBasePrice.doubleValue,
        this.transactionFeeLimit.doubleValue))
      r1   <- Task(31 * r0 + txId.hashCode())
      r2   <- Task(31 * r1 + this.sender.hashCode())
      r3   <- Task(31 * r2 + this.receiver.hashCode())
    } yield r3

  @transient override def hashCode: Int = hashCodeTask.extract

  @transient def apply(blockId: Array[Byte]): Transaction = new Transaction(
    sender                  = this.sender,
    receiver                = this.receiver,
    amount                  = this.amount,
    nonce                   = this.nonce,
    transactionFeeBasePrice = this.transactionFeeBasePrice,
    transactionFeeLimit     = this.transactionFeeLimit,
    blockId                 = blockId
  )

  @transient private val zero: BigDecimal = BigDecimal(0)
}

object Transaction {

  implicit val writes: Writes[Transaction] = (
    (__ \ "sender").write[String] ~
    (__ \ "receiver").write[String] ~
    (__ \ "amount").write[BigDecimal] ~
    (__ \ "nonce").write[Nonce] ~
    (__ \ "transactionFeeBasePrice").write[BigDecimal] ~
    (__ \ "transactionFeeLimit").write[BigDecimal]
  ) (o => (
    new String(o.sender, StandardCharsets.US_ASCII),
    new String(o.receiver, StandardCharsets.US_ASCII),
    o.amount,
    o.nonce,
    o.transactionFeeBasePrice,
    o.transactionFeeLimit)
  )

  implicit val reads: Reads[Transaction] = (
    (__ \ "sender").read[String] ~
    (__ \ "receiver").read[String] ~
    (__ \ "amount").read[BigDecimal] ~
    (__ \ "nonce").read[Nonce] ~
    (__ \ "transactionFeeBasePrice").read[BigDecimal] ~
    (__ \ "transactionFeeLimit").read[BigDecimal]
  ) ((o1, o2, o3, o4, o5, o6) => new Transaction(
    sender                  = o1.getBytes("UTF-8"),
    receiver                = o2.getBytes("UTF-8"),
    amount                  = o3,
    nonce                   = o4,
    transactionFeeBasePrice = o5,
    transactionFeeLimit     = o6
  ))

  implicit val ordering: Ordering[Transaction] = (tx1: Transaction, tx2: Transaction) =>
    if (tx2.transactionFeeBasePrice - tx1.transactionFeeBasePrice < 0.0) -1
    else if (tx1.transactionFeeBasePrice - tx2.transactionFeeBasePrice > 0.0) 1
    else 0

  def apply(
    sender: String,
    receiver: String,
    amount: BigDecimal,
    nonce: Nonce,
    transactionFeeBasePrice: BigDecimal,
    transactionFeeLimit: BigDecimal
  ): Transaction = new Transaction(
    sender = sender.getBytes("UTF-8"),
    receiver = receiver.getBytes("UTF-8"),
    amount = amount,
    nonce = nonce,
    transactionFeeBasePrice = transactionFeeBasePrice,
    transactionFeeLimit = transactionFeeLimit,
    blockId = Array.empty[Byte]
  )

}
