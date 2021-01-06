package core.util.parser

import cats.implicits._
import cats.{Functor, Show}
import play.api.libs.json.{JsError, JsString, JsSuccess, Reads}
import play.api.mvc.{PathBindable, QueryStringBindable}

import scala.util.Try

trait Parse[T] {
  def run(s: String): Either[String, T]
  def validate(implicit validate: Validate[T]): Parse[T] = run(_).flatMap(validate.run)
}

object Parse {

  def parsedPathBindable[T: Show: Parse]: PathBindable[T] = new PathBindable[T] {
    override def bind(k: String, v: String): Either[String, T] =
      implicitly[Parse[T]].run(v).leftFlatMap(x => Left(s"$k.$x"))
    override def unbind(k: String, token: T): String = token.show
  }

  def parsedQueryStringBindable[T: Show: Parse]: QueryStringBindable[T] = new QueryStringBindable[T] {
    private val stringBinder: QueryStringBindable[String] = QueryStringBindable.bindableString
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] =
      stringBinder.bind(key, params).map(_.flatMap(implicitly[Parse[T]].run).leftFlatMap(x => Left(s"$key.$x")))
    override def unbind(key: String, value: T): String = stringBinder.unbind(key, value.show)
  }

  def intParse: Parse[Int] = (s: String) => Try(s.trim.toInt).toEither.leftMap(_ => "parse.expected.int")

  def floatParse: Parse[Float] = (s: String) => Try(s.trim.toFloat).toEither.leftMap(_ => "parse.expected.float")

  def reads[T: Parse]: Reads[T] = Reads[T] {
    case JsString(s) => implicitly[Parse[T]].run(s).fold(JsError(_), JsSuccess(_))
    case _           => JsError("expected.string")
  }

  implicit val functor: Functor[Parse] = new Functor[Parse] {
    override def map[A, B](fa: Parse[A])(f: A => B): Parse[B] = fa.run(_).map(f(_))
  }

}

