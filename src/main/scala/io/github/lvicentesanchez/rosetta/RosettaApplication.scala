package io.github.lvicentesanchez.rosetta

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
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
  implicit val materializer = FlowMaterializer()
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
    enabled = false,
    singleLineMode = true,
    logLevel = 'debug
  )
  //

  val client: AmazonSQSAsync =
    new AmazonSQSAsyncClient(new BasicAWSCredentials(config.sqs.accessKey, config.sqs.secretKey))

  val source: Source[String \/ List[String \/ LocalisedRequest]] =
    Source(() => Iterator.continually(())).mapAsyncUnordered(_ => sqs.produce(config.sqs.queueUrl, client)(20, 10))

  def errorSink[A]: Sink[A] = Sink.foreach[A](err => println(s"::: ERROR ::: $err"))

  val sqlPersistFn: TranslatedRequest => Future[String \/ TranslatedRequest] =
    (a: TranslatedRequest) => Repository.persistTranslation(a)(blocking, AutoSession)

  val sqlRemoveFn: TranslatedRequest => Future[String \/ TranslatedRequest] =
    (a: TranslatedRequest) => Repository.removeTranslation(a)(blocking, AutoSession)

  val translationFn: LocalisedRequest => Future[(String, LocalisedRequest) \/ TranslatedRequest] =
    (a: LocalisedRequest) => Repository.findTranslation(a)(blocking, AutoSession).map(_.bimap((_, a), identity))(system.dispatcher)

  def sqsRemoveFlow[A](fn: A => String): A => Future[(String, A) \/ A] =
    (a: A) => sqs.remove(config.sqs.queueUrl, client)(fn(a)).map(_.bimap((_, a), _ => a))(system.dispatcher)

  import FlowGraphImplicits._

  val graph = FlowGraph { implicit builder =>
    val comm: DisjunctionRoute[String, List[String \/ LocalisedRequest]] = DisjunctionRoute[String, List[String \/ LocalisedRequest]]("comm-error")
    val flat: DisjunctionRoute[String, LocalisedRequest] = DisjunctionRoute[String, LocalisedRequest]("flat-error")
    val loca: DisjunctionRoute[(String, LocalisedRequest), TranslatedRequest] = DisjunctionRoute[(String, LocalisedRequest), TranslatedRequest]("loca-error")
    val unz1: Unzip[String, LocalisedRequest] = Unzip[String, LocalisedRequest]("unz1-error")
    val sqr1: DisjunctionRoute[(String, LocalisedRequest), LocalisedRequest] = DisjunctionRoute[(String, LocalisedRequest), LocalisedRequest]("sqr1-error")
    val dbpr: DisjunctionRoute[String, TranslatedRequest] = DisjunctionRoute[String, TranslatedRequest]("dbpr-error")
    val sqr2: DisjunctionRoute[(String, TranslatedRequest), TranslatedRequest] = DisjunctionRoute[(String, TranslatedRequest), TranslatedRequest]("sqr2-error")
    val unz2: Unzip[String, TranslatedRequest] = Unzip[String, TranslatedRequest]("unz2-error")
    val dbrm: DisjunctionRoute[String, TranslatedRequest] = DisjunctionRoute[String, TranslatedRequest]("dbrm-error")

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

    comm.left ~> errorSink
    comm.right ~> RosettaFlows.flatten[String \/ LocalisedRequest] ~> flat.in

    flat.left ~> errorSink
    flat.right ~> RosettaFlows.mapAsyncUnordered(translationFn) ~> loca.in

    loca.left ~> unz1.in
    loca.right ~> RosettaFlows.mapAsyncUnordered(sqlPersistFn) ~> dbpr.in

    unz1.left ~> errorSink
    unz1.right ~> RosettaFlows.mapAsyncUnordered(sqsRemoveFlow[LocalisedRequest](_.handle)) ~> sqr1.in

    sqr1.left ~> Flow[(String, LocalisedRequest)].map(_._1).to(errorSink)
    sqr1.right ~> Sink.ignore

    dbpr.left ~> errorSink
    dbpr.right ~> RosettaFlows.mapAsyncUnordered(sqsRemoveFlow[TranslatedRequest](_.handle)) ~> sqr2.in

    sqr2.left ~> unz2.in
    sqr2.right ~> Sink.ignore

    unz2.left ~> errorSink
    unz2.right ~> RosettaFlows.mapAsyncUnordered(sqlRemoveFn) ~> dbrm.in

    dbrm.left ~> errorSink
    dbrm.right ~> Sink.ignore
  }

  def start(): Unit = {
    val _ = graph.run()
  }

  def stop(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
