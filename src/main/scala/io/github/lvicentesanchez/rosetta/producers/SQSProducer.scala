package io.github.lvicentesanchez.rosetta.producers

import argonaut.Parse
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.{ ReceiveMessageResult, ReceiveMessageRequest }
import io.github.lvicentesanchez.rosetta.data.Request
import scala.collection.JavaConverters._
import scala.concurrent.{ Promise, Future }
import scalaz.{ -\/, \/, \/- }

trait SQSProducer {
  def produce(queueUrl: String, client: AmazonSQSAsync)(polling: Int, bufferSize: Int): Future[String \/ List[String \/ Request]] = {
    val promise: Promise[String \/ List[String \/ Request]] = Promise[String \/ List[String \/ Request]]()
    try {
      val request: ReceiveMessageRequest =
        new ReceiveMessageRequest().
          withQueueUrl(queueUrl).
          withMaxNumberOfMessages(bufferSize).
          withWaitTimeSeconds(polling)
      client.receiveMessageAsync(request, new AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult] {
        override def onError(exception: Exception): Unit =
          promise.trySuccess(-\/(exception.getMessage))

        override def onSuccess(request: ReceiveMessageRequest, result: ReceiveMessageResult): Unit =
          promise.trySuccess(\/-(
            result.getMessages.iterator.asScala
              .map(msg => Parse.parse(msg.getBody).map(Request(msg.getReceiptHandle, _)))
              .toList
          ))
      } )
    } catch {
      case exception: Throwable => promise.trySuccess(-\/(exception.getMessage))
    }
    promise.future
  }
}
