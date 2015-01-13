package io.github.lvicentesanchez.rosetta.db

import io.github.lvicentesanchez.rosetta.data.{ Translated, LocalisedRequest, TranslatedRequest }
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.\/
import scalaz.syntax.std.option._
import scalikejdbc._

object Repository {
  def findTranslation(data: LocalisedRequest)(implicit ec: ExecutionContext, session: DBSession): Future[String \/ TranslatedRequest] = Future {
    val select: SQL[Nothing, NoExtractor] =
      sql"""
         select message from translation_table where locale = ${data.content.locale} and messageKey = ${data.content.messageKey}
      """
    \/.fromTryCatchThrowable[Option[String], Throwable](select.map(_.string("message")).single().apply())
      .leftMap(_.getMessage)
      .flatMap(_ \/> s"Translation for ${data.content.messageKey} not found for locale ${data.content.locale}!")
      .map(msg => TranslatedRequest(data.handle, Translated(data.content.locale, msg)))
  }

  def persistTranslation(data: TranslatedRequest)(implicit ec: ExecutionContext, session: DBSession): Future[String \/ TranslatedRequest] = Future {
    val insert: SQL[Nothing, NoExtractor] =
      sql"""
         insert into translated_request values(${data.handle}, ${data.content.locale}, ${data.content.message}, current_timestamp)
      """
    \/.fromTryCatchThrowable[Boolean, Throwable](insert.execute().apply()).bimap(_.getMessage, _ => data)
  }

  def removeTranslation(data: TranslatedRequest)(implicit ec: ExecutionContext, session: DBSession): Future[String \/ TranslatedRequest] = Future {
    val delete: SQL[Nothing, NoExtractor] =
      sql"""
         delete From translated_request where handler = ${data.handle}
      """
    \/.fromTryCatchThrowable[Boolean, Throwable](delete.execute().apply()).bimap(_.getMessage, _ => data)
  }
}
