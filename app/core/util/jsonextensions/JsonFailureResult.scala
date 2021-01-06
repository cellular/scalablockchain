package core.util.jsonextensions

import play.api.Logging
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, Conflict, InternalServerError, NotFound}

trait JsonFailureResult extends WritesExtension with Logging {

  implicit class RichThrowableResult[T <: Throwable](error: T) {
    val jsonContentType = "application/json; charset=utf-8"

    def toBadRequestResult(implicit writes: Writes[T]): Result = {
      logger.warn(s"The request is not valid: ${error.getMessage}")
      BadRequest(Json.toJson(error)(writes)).as(jsonContentType)
    }

    def toInternalServerErrorResult(implicit writes: Writes[T]): Result = {
      logger.error(s"An internal server error occurred during request: ${error.getMessage}")
      InternalServerError(Json.toJson(error)(writes)).as(jsonContentType)
    }

    def toNotFoundResult(implicit writes: Writes[T]): Result = {
      logger.debug(s"The requested resource can not be found: ${error.getMessage}")
      NotFound(Json.toJson(error)(writes)).as(jsonContentType)
    }

    def toConflictResult(implicit writes: Writes[T]): Result = {
      logger.warn(s"Processing the request caused a conflict: ${error.getMessage}")
      Conflict(Json.toJson(error)(writes)).as(jsonContentType)
    }
  }

}
