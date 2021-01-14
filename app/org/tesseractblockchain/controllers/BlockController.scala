package org.tesseractblockchain.controllers

import core.util.jsonextensions.{JsonFailureResult, JsonSuccessResult}
import core.{BlockSize, Offset}
import javax.inject.Inject
import org.tesseractblockchain.Application
import org.tesseractblockchain.services.BlockService
import play.api.mvc._

class BlockController @Inject()(
    controllerComponents: ControllerComponents,
    application: Application,
    blockService: BlockService
) extends AbstractController(controllerComponents)
  with JsonSuccessResult
  with JsonFailureResult {

  def getBlockByHash(hex: String): Action[AnyContent] = Action.async { _ =>
    application.runWithZIO(blockService.getBlockByHash(hex).either.map {
      case Right(block) => block.toOkResult
      case Left(err) => err.toInternalServerErrorResult
    })
  }

  def getRecentBlocks(blockSize: BlockSize, offset: Option[Offset]): Action[AnyContent] = Action.async { _ =>
    application.runWithZIO(blockService.getRecentBlocks(blockSize, offset).either map {
      case Right(blocks) => blocks.toOkResult
      case Left(err) => err.toInternalServerErrorResult
    })
  }

  def getChildBlockOfHash(hex: String): Action[AnyContent] = Action.async { _ =>
    application.runWithZIO(blockService.getChildBlockOfHash(hex).either.map {
      case Right(block) => block.toOkResult
      case Left(err) => err.toInternalServerErrorResult
    })
  }

}
