package io.github.lvicentesanchez.rosetta.streams.routes

import akka.stream.FanOutShape2
import akka.stream.scaladsl._
import scalaz.\/

final class DisjunctionRoute[A, B](name: String) extends FlexiRoute[A \/ B, FanOutShape2[A \/ B, A, B]](new FanOutShape2(name), OperationAttributes.name(name)) {
  import FlexiRoute._

  override def createRouteLogic(p: PortT) = new RouteLogic[A \/ B] {

    override def initialState = State(DemandFromAll(p)) { (ctx, _, element) ⇒
      element.fold(ctx.emit(p.out0)(_), ctx.emit(p.out1)(_))
      SameState
    }

    override def initialCompletionHandling = eagerClose
  }
}

object DisjunctionRoute {
  def apply[A, B](name: String): DisjunctionRoute[A, B] = new DisjunctionRoute[A, B](name)
}
