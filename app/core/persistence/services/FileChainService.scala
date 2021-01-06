package core.persistence.services

import java.io.File

import core.NetworkId
import zio.Task

trait FileChainService {

  private val chainPathSkeleton: NetworkId => String = networkId => s"%s/$networkId"

  val blockPath: NetworkId => String => String = networkId => blockId =>
    s"${chainPathSkeleton(networkId)}/$blockId.json"

  val fileTask: String => Task[File] = path => Task(new File(getClass.getResource(path).getPath))

  val chainDoesExist: NetworkId => Task[Boolean] = id => fileTask(chainPathSkeleton(id)).map(_.exists())

  val fnChainPath: String => NetworkId => String = path => chainPathSkeleton(_).format(path)

}
