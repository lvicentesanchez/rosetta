package io.github.lvicentesanchez.rosetta.streams.sources

import akka.stream.scaladsl.{ PropsSource, Source }
import scala.concurrent.Future
import scala.reflect.ClassTag

object concurrent {
  implicit class SourceExtensionMethods(val source: Source.type) extends AnyVal {
    def concurrent[A: ClassTag](concurrency: Long, fn: () => Future[A]): PropsSource[A] =
      Source(ConcurrentPublisher.props(concurrency, fn))
  }

}
