package org.tesseractblockchain.controllers

import core.exceptions.TransactionNotFoundException
import core.util.jsonextensions.{JsonFailureResult, JsonSuccessResult}
import core.{BlockSize, Offset, Transaction}
import javax.inject.Inject
import org.tesseractblockchain.Application
import org.tesseractblockchain.services.TransactionService
import play.api.mvc._

class TransactionController @Inject()(
    controllerComponents: ControllerComponents,
    application: Application,
    transactionService: TransactionService
) extends AbstractController(controllerComponents)
  with JsonSuccessResult
  with JsonFailureResult {

  def getRecentTransactions(blockSize: BlockSize, offset: Offset): Action[AnyContent] = Action.async { _ =>
    application.runWithZIO(transactionService.getRecentTransactions(blockSize, offset).either.map {
      case Right(transactions) => transactions.toOkResult
      case Left(err) => err.toInternalServerErrorResult
    })
  }

  def getTransactionByHash(hex: String): Action[AnyContent] = Action.async { _ =>
    application.runWithZIO(transactionService.getTransactionByHash(hex).either.map {
      case Right(transaction) => transaction.toOkResult
      case Left(err: TransactionNotFoundException) => err.toNotFoundResult
      case Left(err) => err.toInternalServerErrorResult
    })
  }

  def sendTransaction: Action[Transaction] = Action.async(parse.json[Transaction]) { request =>
    application.runWithZIO(transactionService.sendTransaction(request.body).either.map {
      case Right(_) => request.body.toCreatedResult
      case Left(err: TransactionNotFoundException) => err.toNotFoundResult
      case Left(err) => err.toInternalServerErrorResult
    })
  }

}
