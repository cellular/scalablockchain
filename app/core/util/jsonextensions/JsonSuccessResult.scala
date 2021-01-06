package core.util.jsonextensions

import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results._

trait JsonSuccessResult {

  val jsonContentType = "application/json; charset=utf-8"

  implicit class SuccessResultConverter[T](response: T) {

    /**
     * Return a Json OK (HTTP 200).
     */
    def toOkResult(implicit writes: Writes[T]): Result = Ok(Json.toJson(response)(writes)).as(jsonContentType)

    /**
     * Return a Json CREATED (HTTP 201).
     */
    def toCreatedResult(implicit writes: Writes[T]): Result = Created(Json.toJson(response)(writes)).as(jsonContentType)
  }

}

object JsonSuccessResult extends JsonSuccessResult
