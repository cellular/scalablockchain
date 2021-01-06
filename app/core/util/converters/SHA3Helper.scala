package core.util.converters

import java.io.{ByteArrayOutputStream, ObjectOutputStream}

import org.bouncycastle.jcajce.provider.digest.SHA3
import org.bouncycastle.util.encoders.Hex
import zio.Task

import scala.util.Try

object SHA3Helper {

  val digestToHex: Array[Byte] => Task[String] = arr => Task(Hex.toHexString(arr))

  val hash256AsHex: Serializable => Task[String] = o => SHA3Helper.hash256From(o).map(Hex.toHexString)

  val hexToDigest: String => Task[Array[Byte]] = hex => Task(Hex.decode(hex))

  val hash256: Array[Byte] => Task[Array[Byte]] = hash => Task(new SHA3.Digest256().digest(hash))

  def hash256From[T <: Serializable](t: T): Task[Array[Byte]] =
    for {
      baos  <- Task(new ByteArrayOutputStream())
      oos   <- Task(new ObjectOutputStream(baos))
      _     <- Task.fromTry(Try(oos.writeObject(t))) *> Task(oos.close()) *> Task(baos.close())
      bytes <- SHA3Helper.hash256(baos.toByteArray)
    } yield bytes

}
