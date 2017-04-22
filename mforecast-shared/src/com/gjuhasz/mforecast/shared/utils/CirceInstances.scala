package com.gjuhasz.mforecast.shared.utils

import java.time.LocalDate

import com.gjuhasz.mforecast.shared.model.Category
import io.circe._

import scala.util.Try

object CirceInstances {

  implicit val categoryKeyEncoder = new KeyEncoder[Category] {
    override def apply(c: Category): String = c.name
  }
  implicit val fooKeyDecoder = new KeyDecoder[Category] {
    override def apply(key: String): Option[Category] = Some(Category(key))
  }

  private final val DateRegex = """([0-9]{4})-([0-9]{2})-([0-9]{2})""".r

  implicit final val decodeLocalDateDefault: Decoder[LocalDate] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(DateRegex(y, m, d)) =>
          Try { Right(LocalDate.of(y.toInt, m.toInt, d.toInt)) }
            .getOrElse(Left(DecodingFailure("LocalDate", c.history)))
        case Right(_) =>
          Left(DecodingFailure("LocalDate", c.history))
        case l@Left(_) => l.asInstanceOf[Decoder.Result[LocalDate]]
      }
    }

  implicit final val encodeLocalDateDefault: Encoder[LocalDate] =
    Encoder.instance(t => Json.fromString(f"${ t.getYear }%04d-${ t.getMonthValue }%02d-${ t.getDayOfMonth }%02d"))

}
