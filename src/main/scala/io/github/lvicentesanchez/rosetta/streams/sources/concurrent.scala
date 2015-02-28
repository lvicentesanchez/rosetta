package io.github.lvicentesanchez.rosetta.streams.sources

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import scala.concurrent.Future
import scala.reflect.ClassTag

object concurrent {
  implicit class SourceExtensionMethods(val source: Source.type) extends AnyVal {
    def concurrent[A: ClassTag](concurrency: Long, fn: () => Future[A]): Source[A, ActorRef] =
      Source(ConcurrentPublisher.props(concurrency, fn))
  }

}
