package com.gjuhasz.mforecast.shared.utils

import java.nio.ByteBuffer

import fr.hmil.roshttp.body.BulkBodyPart

import scala.language.implicitConversions

class JsonBody private(value: String) extends BulkBodyPart {
  override def contentType: String = s"application/json; charset=utf-8"
  override def contentData: ByteBuffer = ByteBuffer.wrap(value.getBytes("utf-8"))
}
object JsonBody {
  implicit def stringToJsonBody(value: String): JsonBody = new JsonBody(value)
}