package core

import cats.Show
import cats.implicits._
import core.util.parser.Parse
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.QueryStringBindable

final case class BlockSize(value: Int) extends AnyVal

object BlockSize {
  implicit val show: Show[BlockSize] = _.value.toString
  implicit val parse: Parse[BlockSize] = Parse.intParse.map(BlockSize(_))
  implicit val queryBindable: QueryStringBindable[BlockSize] = Parse.parsedQueryStringBindable[BlockSize]
  implicit val writes: Writes[BlockSize] = Writes.of[Int].contramap(_.value)
  implicit val reads: Reads[BlockSize] = Parse.reads[BlockSize]
}
