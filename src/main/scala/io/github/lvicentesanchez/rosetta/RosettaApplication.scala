package io.github.lvicentesanchez.rosetta

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import io.github.lvicentesanchez.daemon.ApplicationLifecycle
import io.github.lvicentesanchez.rosetta.streams.routes.DisjunctionRoute
import io.github.lvicentesanchez.rosetta.streams.sources.concurrent._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.forkjoin.ThreadLocalRandom
import scalaz.{ -\/, \/-, \/ }
import scalaz.std.option._

object RosettaApplication extends ApplicationLifecycle {
  implicit val system = ActorSystem("Sys")
  implicit val materializer = FlowMaterializer()

  val UpperBound: Int = 1000000

  val fn: (ExecutionContext) => Future[String \/ Int] =
    implicit ec =>
      Future.successful {
        val a: String =
          s"Random: ${ThreadLocalRandom.current().nextInt(UpperBound)}"
        val b: Int =
          ThreadLocalRandom.current().nextInt(UpperBound)
        if (math.random < 0.15) -\/(a) else \/-(b)
      }
  val maxRandomNumberSize = 1000000
  val tupleSource =
    Source.concurrent(16, none[ExecutionContext], fn)
  //Source(() => Iterator.continually(())).mapAsync(_ => fn(system.dispatcher))

  val leftSink: Sink[String] = Sink.foreach(println)

  val rightSink: Sink[Int] = Sink.foreach(println)

  import FlowGraphImplicits._

  val graph = FlowGraph { implicit builder =>
    val dj: DisjunctionRoute[String, Int] = new DisjunctionRoute[String, Int]

    tupleSource ~> dj.in
    dj.left ~> leftSink
    dj.right ~> rightSink
  }

  def start(): Unit = {
    val _ = graph.run()
  }

  def stop(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
