package core.persistence.services

import java.io.{File, PrintWriter}

import cats.implicits._
import core.persistence.errors.FileWritingBlockThrowable
import core.util.converters.SHA3Helper
import core.{Block, Chain}
import play.api.libs.json.Json
import zio.interop.catz._
import zio.{Task, ZIO}

import scala.util.Try

class FileWriteChainService extends FileChainService {

  def writeChain(chain: Chain, path: String): Task[Unit] =
    for {
      networkId      <- Task.succeed(chain.networkId)
      _              <- ZIO.unlessM(chainDoesExist(networkId))(
                          fileTask.compose(fnChainPath(path))(networkId).map(_.mkdir)
                        )
      hashBlockTuple <- chain.blocks.traverse { block =>
                          block.getBlockHash.flatMap(SHA3Helper.digestToHex(_).map(_ -> block))
                        }
      fnFileTask      = fileTask.compose(blockPath(networkId))(_)
      _              <- hashBlockTuple.traverse { case (id, block) =>
                          ZIO.unlessM(fnFileTask(id).map(_.exists()))(writeBlock(block)(fnFileTask(id)))
                        }
    } yield ()

  private def writeBlock(block: Block): Task[File] => Task[Unit] = _.flatMap { file =>
    (for {
      writer <- Task(new PrintWriter(file))
      json   <- Task(Json.toJson(block)).map(Json.stringify)
      _      <- Task.fromTry(Try(writer.write(json))) *> Task(writer.close())
    } yield ()).mapError(FileWritingBlockThrowable)
  }

}
