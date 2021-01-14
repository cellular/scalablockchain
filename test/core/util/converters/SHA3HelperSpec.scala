package core.util.converters

import org.bouncycastle.util.encoders.Hex
import tests.TestSpec

class SHA3HelperSpec extends TestSpec {

  trait TestSetup {
    val hashDigits: Array[Byte] =
      Array(-24, 105, -111, -15, -120, 86, -17, 25, -124, -68, -94, 100, 92, 15, 123, 63, -112, -111, -76, 24, 87, 107,
        43, -61, -108, -36, 53, -2, 86, 123, -2, -57)
    val sha3hashDigits: Array[Byte] =
      Array(4, -113, -94, -56, -29, 4, 55, 11, -28, -108, 40, 39, 47, -80, 37, 9, -58, 72, 6, -77, 11, 0, 10, 72, -105,
        -90, 124, 111, -27, -24, 10, -69)
    val hashHexStr = "a93c3758a9689d3b0a9489b0d8d2af370946269e82a2cb4015bab28865778175"
    val sha3HashStr = "048fa2c8e304370be49428272fb02509c64806b30b000a4897a67c6fe5e80abb"
  }

  "SHA3Helper#hash256" must {
    "return a hash as hex string" in new TestSetup {
      testZIO(SHA3Helper.hash256(hashDigits)) {
        case Right(arr) => arr mustEqual sha3hashDigits
        case Left(_) => fail("hash256")
      }
    }
  }

  "SHA3Helper#digestToHex" must {
    "return a hash as hex string" in new TestSetup {
      testZIO(SHA3Helper.digestToHex(sha3hashDigits))(_ mustBe Right(sha3HashStr))
    }
  }

  "SHA3Helper#hexToDigest" must {
    "return a hash as hex string" in new TestSetup {
      testZIO(SHA3Helper.hexToDigest(sha3HashStr)) {
        case Right(arr) => arr mustEqual sha3hashDigits
        case Left(_) => fail("hexToDigest")
      }
    }
  }

  "SHA3Helper#hash256AsHex" must {
    "return a hash as hex string" in new TestSetup {
      testZIO(SHA3Helper.hash256AsHex(sha3hashDigits))(_ mustBe Right(hashHexStr))
    }
  }

  "SHA3Helper#hash256From" must {
    "return hash as digits of a bytes array" in new TestSetup {
      testZIO(SHA3Helper.hash256From(sha3hashDigits)) {
        case Right(bytes) => Hex.toHexString(bytes) mustBe hashHexStr
        case Left(_) => fail("hash256From")
      }
    }
  }

}
