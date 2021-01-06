package core

import play.api.libs.json.Writes

final case class NetworkId(value: Int) extends AnyVal

object NetworkId {
  implicit val writes: Writes[NetworkId] = Writes.of[Int].contramap(_.value)
}
