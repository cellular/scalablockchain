package core.persistence.services

import java.io.File

import cats.implicits._
import core.persistence.errors.{FileReadingBlockThrowable, ChainNotFoundThrowable}
import core.util.catz.MonadExtension
import core.{Block, Chain, NetworkId}
import play.api.libs.json.Json
import zio.interop.catz._
import zio.{Task, ZIO}

import scala.io.Source
import scala.util.Try

class FileReadChainService extends FileChainService {

  def readChain(networkId: NetworkId, path: String): Task[Chain] =
    ZIO.ifM(chainDoesExist(networkId))(
      readChain(Chain(networkId))(fileTask.compose(fnChainPath(path))(networkId)),
      Task.fail(ChainNotFoundThrowable)
    )

  private def readChain(chain: Chain): Task[File] => Task[Chain] = file =>
    for {
      files  <- file.map(_.listFiles().to(List))
      blocks <- files.traverse(readBlock)
    } yield chain.copy(blocks = blocks.flatMap(chain.addBlock))

  private def readBlock(file: File): Task[Block] =
    MonadExtension.foldOptionM[Task, Block](for {
      source   <- Task(Source.fromFile(file))
      lines    <- Task.fromTry(Try(source.getLines))
      asString <- Task.succeed(lines.mkString)
      block    <- Task(source.close()) *> Task(Json.parse(asString).asOpt[Block])
    } yield block, Task.fail(FileReadingBlockThrowable(file)))

}
