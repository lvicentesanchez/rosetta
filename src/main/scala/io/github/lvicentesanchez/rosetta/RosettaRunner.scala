package io.github.lvicentesanchez.rosetta

import io.github.lvicentesanchez.daemon.ApplicationRunner

case object RosettaRunner extends ApplicationRunner {
  def createApplication() = new RosettaDaemon()
}
