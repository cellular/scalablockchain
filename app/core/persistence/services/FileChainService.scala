package core.persistence.services

import java.io.File

import cats.implicits._
import core.NetworkId
import zio.Task

trait FileChainService extends {

  private type Path = String

  private val chainPathSkeleton: NetworkId => String = networkId => s"%s/${networkId.show}"

  val blockPath: Path => NetworkId => String => String = path => networkId => blockId =>
    s"${chainPathSkeleton(networkId).format(path)}/$blockId.json"

  val fileTask: Path => Task[File] = path => Task(new File(path))

  val chainDoesExist: Path => NetworkId => Task[Boolean] = path => networkId =>
    fileTask(chainPathSkeleton(networkId).format(path)).map(_.exists())

  val fnChainPath: Path => NetworkId => String = path => chainPathSkeleton(_).format(path)

}
