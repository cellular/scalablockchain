package core.util.jsonextensions

import play.api.libs.json.{Writes, __}

trait WritesExtension {
  implicit def writes[A <: Throwable]: Writes[A] = (__ \ "error").write[String].contramap(_.getMessage)
}

object WritesExtension extends WritesExtension
