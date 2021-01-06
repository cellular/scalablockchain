package org.tesseractblockchain.controllers

import core.util.jsonextensions.{JsonFailureResult, JsonSuccessResult}
import javax.inject.Inject
import org.tesseractblockchain.Application
import org.tesseractblockchain.services.DispatcherService
import play.api.mvc._

class DispatcherController @Inject()(
    controllerComponents: ControllerComponents,
    application: Application,
    dispatcherService: DispatcherService
) extends AbstractController(controllerComponents)
  with JsonSuccessResult
  with JsonFailureResult {

  def searchForHash(hex: String): Action[AnyContent] = Action.async { _ =>
    application.liftZIO(dispatcherService.searchForHash(hex).either.map {
      case Right(response) => response.toOkResult
      case Left(err) => err.toNotFoundResult
    })
  }

}
