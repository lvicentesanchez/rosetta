package io.github.lvicentesanchez.rosetta

import argonaut._, Argonaut._
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import com.amazonaws.services.sqs.{ AmazonSQSAsyncClient, AmazonSQSAsync }
import io.github.lvicentesanchez.daemon.ApplicationLifecycle
import io.github.lvicentesanchez.rosetta.data.Request
//import io.github.lvicentesanchez.rosetta.producers.amazon.sqs
import io.github.lvicentesanchez.rosetta.streams.routes.DisjunctionRoute
import io.github.lvicentesanchez.rosetta.streams.sources.concurrent._
//import io.github.lvicentesanchez.rosetta.streams.stages.FlattenStage
import java.util.UUID
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz.{ \/, \/- }

object RosettaApplication extends ApplicationLifecycle {
  implicit val system = ActorSystem("rosetta")
  implicit val materializer = FlowMaterializer()

  val client: AmazonSQSAsync =
    new AmazonSQSAsyncClient()

  val fn: () => Future[String \/ List[String \/ Request]] =
    () =>
      Future.successful(
        \/-(Iterator.range(0, 10).map(i => \/-(Request(UUID.randomUUID().toString, jString(s"Message $i :: ${DateTime.now}")))).toList)
      )
  //() => sqs.produce("", new AmazonSQSAsyncClient())(20, 10)

  val source: Source[String \/ List[String \/ Request]] =
    Source.concurrent(16, fn)
  //Source(() => Iterator.continually(())).mapAsync(_ => fn(system.dispatcher))

  def errorSink: Sink[String] = Sink.foreach(println)

  val rightFlow: Flow[List[String \/ Request], String \/ Request] =
    Flow[List[String \/ Request]].
      mapConcat(identity)
  //transform(() => FlattenStage[List, Request])

  val rightSink: Sink[Request] = Sink.foreach(println)

  import FlowGraphImplicits._

  val graph = FlowGraph { implicit builder =>
    val dj0: DisjunctionRoute[String, List[String \/ Request]] = DisjunctionRoute[String, List[String \/ Request]]("disjunction-0")
    val dj1: DisjunctionRoute[String, Request] = DisjunctionRoute[String, Request]("disjunction-1")

    source ~> dj0.in

    dj0.left ~> errorSink
    dj0.right ~> rightFlow ~> dj1.in

    dj1.left ~> errorSink
    dj1.right ~> rightSink
  }

  def start(): Unit = {
    val _ = graph.run()
  }

  def stop(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
