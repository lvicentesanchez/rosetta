package io.github.lvicentesanchez.rosetta

import argonaut._

package object json {
  implicit class ACursorExtension(val cursor: ACursor) extends AnyVal {
    def asOpt[A](implicit e: DecodeJson[A]): Option[A] =
      cursor.as[A].toOption
  }
}
