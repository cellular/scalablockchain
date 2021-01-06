package core

import cats.implicits._
import cats.Show
import core.util.parser.{Parse, Validate}
import play.api.libs.json.Reads
import play.api.mvc.QueryStringBindable

final case class Offset(value: Int) extends AnyVal

object Offset {
  implicit val show: Show[Offset] = _.value.toString
  implicit val validate: Validate[Offset] = Validate.min(0).contramap(_.value)
  implicit val parse: Parse[Offset] = Parse.intParse.map(Offset(_)).validate
  implicit val queryBindable: QueryStringBindable[Offset] = Parse.parsedQueryStringBindable[Offset]
  implicit val reads: Reads[Offset] = Validate.reads(Reads.of[Int].map(Offset(_)))
  val default: Offset = Offset(0)
}
