package core

import core.util.converters.SHA3Helper
import play.api.libs.json.{Json, Reads, Writes}
import zio.Task

/**
 * TODO add difficulty
 */
final case class BlockHeader(
    timestamp: Long,
    previousHash: Array[Byte],
    transactionListHash: Array[Byte]
) extends Serializable {
  val version: Version = Version(1)
  val nonce: Nonce = Nonce(0)

  @transient val asHash: Task[Array[Byte]] = SHA3Helper.hash256From[BlockHeader](this)
  @transient def incrementNonce: Task[Nonce] = Task(Nonce(this.nonce.value + 1))
}

object BlockHeader {
  implicit val writes: Writes[BlockHeader] = Json.writes[BlockHeader]
  implicit val reads: Reads[BlockHeader] = Json.reads[BlockHeader]
}