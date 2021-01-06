package core

import cats.Show
import play.api.libs.json.Writes

final case class NetworkId(value: Int) extends AnyVal

object NetworkId {
  implicit val show: Show[NetworkId] = _.value.toString
  implicit val writes: Writes[NetworkId] = Writes.of[Int].contramap(_.value)
}
