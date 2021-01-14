package core.fixtures

import com.danielasfregola.randomdatagenerator.RandomDataGenerator
import core.{Block, Transaction}
import core.generators.CoreGenerators
import org.scalacheck.Arbitrary

trait CoreFixtures extends CoreGenerators {

  implicit val arbitraryTransaction: Arbitrary[Transaction] = Arbitrary(genTransaction)
  implicit val arbitraryHex64Str: Arbitrary[String] = Arbitrary(genHex64Str)
  implicit val arbitraryBlock: Arbitrary[Block] = Arbitrary(genBlock)

  val transaction1 :: transaction2 :: Nil = RandomDataGenerator.random[Transaction](2).to(List)
  val hashStr1 :: hashStr2 :: hashStr3:: Nil = RandomDataGenerator.random[String](3).to(List)
  val block1 :: block2 :: block3 :: Nil = RandomDataGenerator.random[Block](3).to(List)

}

object CoreFixtures extends CoreFixtures
