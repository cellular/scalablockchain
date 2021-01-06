package core

import play.api.libs.json.{Reads, Writes}

final case class TransactionCount(value: Int) extends AnyVal

object TransactionCount {
  implicit val writes: Writes[TransactionCount] = Writes.of[Int].contramap(_.value)
  implicit val reads: Reads[TransactionCount] = Reads.of[Int].map(TransactionCount(_))
}
