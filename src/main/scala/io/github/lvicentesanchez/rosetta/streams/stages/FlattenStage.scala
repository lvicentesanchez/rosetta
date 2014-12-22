package io.github.lvicentesanchez.rosetta.streams.stages

import akka.stream.stage._

object FlattenStage {
  def apply[CC[X] <: Iterable[X], A]: Stage[CC[A], A] = new StatefulStage[CC[A], A] {
    override def initial: StageState[CC[A], A] = new State {
      override def onPush(elem: CC[A], ctx: Context[A]): Directive = emit(elem.iterator, ctx)
    }
  }
}
