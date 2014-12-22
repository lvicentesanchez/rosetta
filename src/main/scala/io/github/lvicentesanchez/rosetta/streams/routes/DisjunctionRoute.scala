package io.github.lvicentesanchez.rosetta.streams.routes

import akka.stream.scaladsl._
import scala.collection.immutable
import scalaz.\/

final class DisjunctionRoute[A, B] extends FlexiRoute[A \/ B]("disjunction") {
  import FlexiRoute._

  val left = createOutputPort[A]()
  val right = createOutputPort[B]()

  override def createRouteLogic() = new RouteLogic[A \/ B] {
    override def outputHandles(outputCount: Int): immutable.IndexedSeq[OutputHandle] = {
      require(outputCount == 2, s"Disjunction must have two connected outputs, was $outputCount")
      Vector(left, right)
    }
    override def initialState = State[Any](DemandFromAll(left, right)) { (context, _, element) ⇒
      element.fold(context.emit(left, _), context.emit(right, _))
      SameState
    }
    override def initialCompletionHandling = eagerClose
  }
}
