package io.github.lvicentesanchez.rosetta.streams.sources

import akka.pattern.pipe
import akka.actor.{ ActorLogging, Props }
import akka.stream.actor.{ ActorPublisherMessage, ActorPublisher }
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag
import scalaz.std.boolean._
import scalaz.syntax.std.option._

// Produce N elements in parallel
//
final class ConcurrentPublisher[A: ClassTag](concurrency: Long, ec: Option[ExecutionContext], fn: (ExecutionContext) => Future[A]) extends ActorPublisher[A] with ActorLogging {
  import context.dispatcher
  override val receive: Receive =
    work(0)

  def work(pending: Long): Receive = {
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
      val produce: Long = math.min(concurrency, totalDemand) - pending
      Iterator.fill(produce.toInt)(fn(ec | context.dispatcher)).foreach(_.pipeTo(context.self))
      context.become(work(pending + produce))
    }
  }
}

object ConcurrentPublisher {
  def props[A: ClassTag](concurrency: Long, ec: Option[ExecutionContext], fn: (ExecutionContext) => Future[A]) =
    Props(new ConcurrentPublisher(concurrency, ec, fn))
}
