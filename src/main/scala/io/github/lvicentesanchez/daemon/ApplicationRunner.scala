package io.github.lvicentesanchez.daemon

import java.util.concurrent.atomic.AtomicBoolean

abstract class ApplicationRunner extends App {
  def createApplication(): AbstractApplicationDaemon

  private[this] val cleanupAlreadyRun: AtomicBoolean = new AtomicBoolean(false)

  private[this] def cleanup(): Unit = {
    if (cleanupAlreadyRun.compareAndSet(false, true)) {
      application.stop()
    }
  }

  private[this] val application = createApplication()

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    def run(): Unit = {
      cleanup()
    }
  }))

  application.start()
  Console.in.readLine()
  cleanup()
  sys.exit(0)
}
