package io.github.lvicentesanchez.rosetta.streams.sources

import akka.pattern.pipe
import akka.actor.{ ActorLogging, Props }
import akka.stream.actor.{ ActorPublisherMessage, ActorPublisher }
import scala.concurrent.Future
import scala.reflect.ClassTag
import scalaz.std.boolean._

// Produce N elements in parallel
//
final class ConcurrentPublisher[A: ClassTag](concurrency: Long, fn: () => Future[A]) extends ActorPublisher[A] with ActorLogging {
  override val receive: Receive =
    loop(0)

  def loop(pending: Long): Receive = {
    case ActorPublisherMessage.Request(_) =>
      generate(pending)

    case ActorPublisherMessage.Cancel =>
    // do nothing

    case element: A =>
      when(isActive) {
        onNext(element)
        generate(pending - 1)
      }
  }

  def generate(pending: Long): Unit = {
    when(isActive && totalDemand > 0) {
      import context.dispatcher
      val produce: Long = math.min(concurrency, totalDemand) - pending
      Iterator.fill(produce.toInt)(fn()).foreach(_.pipeTo(context.self))
      context.become(loop(pending + produce))
    }
  }
}

object ConcurrentPublisher {
  def props[A: ClassTag](concurrency: Long, fn: () => Future[A]) =
    Props(new ConcurrentPublisher(concurrency, fn))
}
