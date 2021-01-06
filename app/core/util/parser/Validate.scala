package core.util.parser

import cats.implicits._
import cats.{Contravariant, Monoid}
import play.api.libs.json.Reads

trait Validate[T] {
  def run(s: T): Either[String, T]
}

object Validate {

  private val regexDefaultMoney = """^?([0-9])+\.([0-9]{0,2})?$""".r

  val defaultMoneyFromString: Validate[String] = {
    case s@regexDefaultMoney(_, _) => Right(s)
    case _ => Left("parse.format.default.money")
  }

  val defaultMoneyFromFloat: Validate[Float] = d => defaultMoneyFromString.run(d.toString).map(_.toFloat)

  def reads[T: Validate](reads: Reads[T]): Reads[T] =
    reads.flatMap(result => implicitly[Validate[T]].run(result).fold(Reads.failed(_), Reads.pure(_)))

  def min[T: Ordering](x: T): Validate[T] = y => Either.cond(
    test  = implicitly[Ordering[T]].lteq(x, y),
    right = y,
    left  = "validate.range.min"
  )

  def max[T: Ordering](x: T): Validate[T] = y => Either.cond(
    test  = implicitly[Ordering[T]].gteq(x, y),
    right = y,
    left  = "validate.range.max"
  )

  implicit def monoid[T]: Monoid[Validate[T]] = new Monoid[Validate[T]] {
    override def empty: Validate[T] = x => Right(x)
    override def combine(x: Validate[T], y: Validate[T]): Validate[T] = x.run(_).flatMap(y.run)
  }

  implicit val cofunctor: Contravariant[Validate] = new Contravariant[Validate] {
    override def contramap[A, B](fa: Validate[A])(f: B => A): Validate[B] = x => fa.run(f(x)) >> Right(x)
  }

}
