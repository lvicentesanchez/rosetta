package io.github.lvicentesanchez.rosetta

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.{ AmazonSQSAsyncClient, AmazonSQSAsync }
import com.typesafe.config.ConfigFactory
import io.github.lvicentesanchez.daemon.ApplicationLifecycle
import io.github.lvicentesanchez.rosetta.config.RosettaConfig
import io.github.lvicentesanchez.rosetta.data.{ LocalisedRequest, TranslatedRequest }
import io.github.lvicentesanchez.rosetta.db.Repository
import io.github.lvicentesanchez.rosetta.producers.amazon.sqs
import io.github.lvicentesanchez.rosetta.streams.routes.DisjunctionRoute
import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.\/
import scalikejdbc._

object RosettaApplication extends ApplicationLifecycle {
  val blocking: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(32))
  val config: RosettaConfig = RosettaConfig.reader(ConfigFactory.load())
  implicit val system = ActorSystem("rosetta")
  implicit val materializer = ActorFlowMaterializer()
  //
  // Db initialisation
  //
  Class.forName("org.h2.Driver")

  ConnectionPool.singleton(
    config.db.url,
    config.db.user,
    config.db.pass,
    ConnectionPoolSettings(connectionPoolFactoryName = "commons-dbcp")
  )

  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
    enabled = false
  )
  //

  val client: AmazonSQSAsync =
    new AmazonSQSAsyncClient(new BasicAWSCredentials(config.sqs.accessKey, config.sqs.secretKey))

  val source: Source[String \/ List[String \/ LocalisedRequest], Unit] =
    Source(() => Iterator.continually(())).mapAsyncUnordered(_ => sqs.produce(config.sqs.queueUrl, client)(20, 10))

  def errorSink[A]: Sink[A, Future[Unit]] = Sink.foreach[A](err => println(s"::: ERROR ::: $err"))

  val sqlPersistFn: TranslatedRequest => Future[String \/ TranslatedRequest] =
    (a: TranslatedRequest) => Repository.persistTranslation(a)(blocking, AutoSession)

  val sqlRemoveFn: TranslatedRequest => Future[String \/ TranslatedRequest] =
    (a: TranslatedRequest) => Repository.removeTranslation(a)(blocking, AutoSession)

  val translationFn: LocalisedRequest => Future[(String, LocalisedRequest) \/ TranslatedRequest] =
    (a: LocalisedRequest) => Repository.findTranslation(a)(blocking, AutoSession).map(_.bimap((_, a), identity))(system.dispatcher)

  def sqsRemoveFlow[A](fn: A => String): A => Future[(String, A) \/ A] =
    (a: A) => sqs.remove(config.sqs.queueUrl, client)(fn(a)).map(_.bimap((_, a), _ => a))(system.dispatcher)

  val graph = FlowGraph.closed() { implicit builder =>
    import FlowGraph.Implicits._
    val comm = builder.add(
      DisjunctionRoute[String, List[String \/ LocalisedRequest]]("comm-error")
    )
    val flat = builder.add(
      DisjunctionRoute[String, LocalisedRequest]("flat-error")
    )
    val loca = builder.add(
      DisjunctionRoute[(String, LocalisedRequest), TranslatedRequest]("loca-error")
    )
    val unz1 = builder.add(
      Unzip[String, LocalisedRequest]()
    )
    val sqr1 = builder.add(
      DisjunctionRoute[(String, LocalisedRequest), LocalisedRequest]("sqr1-error")
    )
    val dbpr = builder.add(
      DisjunctionRoute[String, TranslatedRequest]("dbpr-error")
    )
    val sqr2 = builder.add(
      DisjunctionRoute[(String, TranslatedRequest), TranslatedRequest]("sqr2-error")
    )
    val unz2 = builder.add(
      Unzip[String, TranslatedRequest]()
    )
    val dbrm = builder.add(
      DisjunctionRoute[String, TranslatedRequest]("dbrm-error")
    )

    //
    //                                      - logerro     - logerro                  - logerro    - logerro
    //                                     /             /                          /            /
    //                                    /()           /\/                        /()          /\/
    //           - logerro   - logerro    --- sqsRemove --- ignore   - logerror    --- dbRemove --- ignore
    //          /           /            /                          /             /
    //         /\/         /\/          /\/                        /\/           /\/
    //  source --- flatten --- localise --- dbPersist ---------------- sqsRemove --- ignore
    //

    source ~> comm.in

    comm.out0 ~> errorSink
    comm.out1 ~> RosettaFlows.flatten[String \/ LocalisedRequest] ~> flat.in

    flat.out0 ~> errorSink
    flat.out1 ~> RosettaFlows.mapAsyncUnordered(translationFn) ~> loca.in

    loca.out0 ~> unz1.in
    loca.out1 ~> RosettaFlows.mapAsyncUnordered(sqlPersistFn) ~> dbpr.in

    unz1.out0 ~> errorSink
    unz1.out1 ~> RosettaFlows.mapAsyncUnordered(sqsRemoveFlow[LocalisedRequest](_.handle)) ~> sqr1.in

    sqr1.out0 ~> Flow[(String, LocalisedRequest)].map(_._1).to(errorSink)
    sqr1.out1 ~> Sink.ignore

    dbpr.out0 ~> errorSink
    dbpr.out1 ~> RosettaFlows.mapAsyncUnordered(sqsRemoveFlow[TranslatedRequest](_.handle)) ~> sqr2.in

    sqr2.out0 ~> unz2.in
    sqr2.out1 ~> Sink.ignore

    unz2.out0 ~> errorSink
    unz2.out1 ~> RosettaFlows.mapAsyncUnordered(sqlRemoveFn) ~> dbrm.in

    dbrm.out0 ~> errorSink
    dbrm.out1 ~> Sink.ignore
  }

  def start(): Unit = {
    val _ = graph.run()
  }

  def stop(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
