package org.tesseractblockchain.models

import core.{Block, Transaction}
import play.api.libs.json.{Json, Writes}

private[tesseractblockchain] final case class SearchForHashResponse(
    block: Block,
    transaction: Transaction
)

private[tesseractblockchain] object SearchForHashResponse {
  implicit val writes: Writes[SearchForHashResponse] = Json.writes[SearchForHashResponse]
}
