package io.github.lvicentesanchez.rosetta

import io.github.lvicentesanchez.daemon.{ AbstractApplicationDaemon, ApplicationLifecycle }

class RosettaDaemon() extends AbstractApplicationDaemon {
  val application: ApplicationLifecycle = RosettaApplication
}
