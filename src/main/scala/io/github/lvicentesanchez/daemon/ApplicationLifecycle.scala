package io.github.lvicentesanchez.daemon

trait ApplicationLifecycle {
  def start(): Unit
  def stop(): Unit
}

