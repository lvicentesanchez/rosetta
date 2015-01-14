package io.github.lvicentesanchez.rosetta

import akka.stream.scaladsl.Flow

import scala.concurrent.Future
import scalaz.\/

object RosettaFlows {
  def flatten[A]: Flow[List[A], A] =
    Flow[List[A]].
      filter(_.nonEmpty).
      mapConcat(identity)

  def mapAsyncUnordered[A, B, C](fn: A => Future[B \/ C]): Flow[A, B \/ C] =
    Flow[A].
      mapAsyncUnordered(fn)
}
