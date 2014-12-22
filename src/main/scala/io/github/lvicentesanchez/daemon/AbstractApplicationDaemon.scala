package io.github.lvicentesanchez.daemon

import org.apache.commons.daemon.{ Daemon, DaemonContext }

abstract class AbstractApplicationDaemon extends Daemon {
  def application: ApplicationLifecycle

  def init(daemonContext: DaemonContext): Unit = {}

  def start(): Unit = application.start()

  def stop(): Unit = application.stop()

  def destroy(): Unit = application.stop()
}

