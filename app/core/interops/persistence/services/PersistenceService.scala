package core.interops.persistence.services

import core.persistence.services.{FileReadChainService, FileWriteChainService}
import core.{Chain, NetworkId}
import javax.inject.Inject
import zio.Task
import zio.interop.catz._

class PersistenceService @Inject()(
    fileReadChainService: FileReadChainService,
    fileWriteChainService: FileWriteChainService
) {

  private val baseRoot = s"${System.getProperty("user.home")}/tesseractblockchain/resources/chain"

  def writeChain(chain: Chain): Task[Unit] = fileWriteChainService.writeChain(chain, baseRoot)

  def readChain(networkId: NetworkId): Task[Chain] = fileReadChainService.readChain(networkId, baseRoot)

}
