package io.github.lvicentesanchez.rosetta.data

import argonaut._, Argonaut._

case class Localised(locale: String, messageKey: String)

object Localised {
  implicit val codec: CodecJson[Localised] = casecodec2(Localised.apply _, Localised.unapply _)("locale", "messageKey")
}
