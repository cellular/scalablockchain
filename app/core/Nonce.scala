package core

import play.api.libs.json.{Reads, Writes}

final case class Nonce(value: Int) extends AnyVal

object Nonce {
  implicit val writes: Writes[Nonce] = Writes.of[Int].contramap(_.value)
  implicit val reads: Reads[Nonce] = Reads.of[Int].map(Nonce(_))
}
