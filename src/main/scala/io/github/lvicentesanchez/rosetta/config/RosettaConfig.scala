package io.github.lvicentesanchez.rosetta.config

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import scalaz.Reader

case class RosettaConfig(db: DbConfig, sqs: SQSConfig)

object RosettaConfig {
  val reader: Reader[Config, RosettaConfig] =
    for {
      db <- Reader[Config, DbConfig](_.as[DbConfig]("rosetta.db"))
      sqs <- Reader[Config, SQSConfig](_.as[SQSConfig]("rosetta.sqs"))
    } yield RosettaConfig(db, sqs)
}