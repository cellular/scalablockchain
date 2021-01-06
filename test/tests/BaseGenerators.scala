package tests

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

trait BaseGenerators {

  val genHexadecimalChar: Gen[Char] = Gen.oneOf(
    Gen.oneOf("0123456789abcdef".toSeq),
    Gen.oneOf("0123456789ABCDEF".toSeq)
  )

  val genPosInteger: Gen[Int] = Gen.choose(0, Int.MaxValue)

  val genNonEmptyString: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)

  def genNonEmptyHexString(l: Int): Gen[String] = Gen.listOfN(l, genHexadecimalChar).map(_.mkString)

  def genEvenOption[T](gen: Gen[T]): Gen[Option[T]] = Gen.frequency(1 -> Gen.const(None), 1 -> Gen.some(gen))

  val genFloatWithFractionalNotZero: Gen[Float] = arbitrary[Float].suchThat(f => f != Math.ceil(f))

  def mkEvenOptionGen[T](gen: Gen[T]): Gen[Option[T]] = Gen.frequency(1 -> Gen.const(None), 1 -> Gen.some(gen))

}

object BaseGenerators extends BaseGenerators
