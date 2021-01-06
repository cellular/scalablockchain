package core

import java.time.Instant

import tests.TestSpec
import zio.Task

class BlockHeaderSpec extends TestSpec {

  private val sha3hashDigits: Array[Byte] =
    Array(4, -113, -94, -56, -29, 4, 55, 11, -28, -108, 40, 39, 47, -80, 37, 9, -58, 72, 6, -77, 11, 0, 10, 72, -105,
      -90, 124, 111, -27, -24, 10, -69)

  "BlockHeader#asHash" must {
    "return a hash of BlockHeader" in {
      val blockHeader = BlockHeader(Instant.now.toEpochMilli, sha3hashDigits, sha3hashDigits)
      val hash: Task[Array[Byte]] = blockHeader.asHash

      val program = for {
        hash0 <- blockHeader.asHash
        hash1 <- hash
      } yield hash0 -> hash1

      whenReady(program) {
        case Right((h1, h2)) => h1 mustEqual h2
        case Left(_) => fail("unexpected error: asHash")
      }
    }
  }

  "BlockHeader#incrementNonce" must {
    "return a incremented Nonce" in {
      val blockHeader = BlockHeader(Instant.now.toEpochMilli, sha3hashDigits, sha3hashDigits)
      whenReady(blockHeader.incrementNonce)(_ mustBe Right(Nonce(1)))
    }
  }

}
