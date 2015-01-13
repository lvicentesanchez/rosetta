package io.github.lvicentesanchez.rosetta.config

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

case class SQSConfig(queueUrl: String, accessKey: String, secretKey: String)

object SQSConfig {
  implicit val valueReader: ValueReader[SQSConfig] = ValueReader.relative { config =>
    SQSConfig(
      config.as[String]("queueUrl"),
      config.as[String]("accessKey"),
      config.as[String]("secretKey")
    )
  }
}