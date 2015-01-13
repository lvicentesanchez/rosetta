package io.github.lvicentesanchez.rosetta.config

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

case class DbConfig(url: String, user: String, pass: String)

object DbConfig {
  implicit val valueReader: ValueReader[DbConfig] = ValueReader.relative { config =>
    DbConfig(
      config.as[String]("url"),
      config.as[String]("user"),
      config.as[String]("pass")
    )
  }
}