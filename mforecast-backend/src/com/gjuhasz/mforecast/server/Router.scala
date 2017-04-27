package com.gjuhasz.mforecast.server

import java.time.{LocalDateTime, Period}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.gjuhasz.mforecast.lib.Mfc
import com.gjuhasz.mforecast.shared.model._
import de.heikoseeberger.akkahttpcirce._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import com.gjuhasz.mforecast.shared.utils.CirceInstances._
import com.typesafe.scalalogging.LazyLogging
//import io.circe.syntax._ // keep

object Router extends LazyLogging {

  import akka.http.scaladsl.server.Directives.{pathPrefix => p}

  def post0(str: String)(f: => Route) = pathPrefix(str) {
    pathEnd {
      post {
        f
      }
    }
  }
  def get0(str: String)(f: => Route) = pathPrefix(str) {
    pathEnd {
      get {
        f
      }
    }
  }

  val route =
    p("api") {
      get0("heartbeat") {
        val time = LocalDateTime.now()
        logger.info(s"Sending heartbeat $time")
        complete(s"Server time is: $time}")
      } ~
        post0("plan") {
          entity(as[MfcArgs]) { mfcArgs =>
            val id = uuid()
            logger.info(s"[$id] Started calculation")
            import mfcArgs._
            logger.info(s"[$id] Start: $start, Size: ${ cashflows.size }")
            val res = Mfc.plan(start, cashflows, defaultAccount, allocated.withDefaultValue(0))
            logger.info(s"[$id] Finished calculation")
            complete(res)
          }
        }
    }

  private def uuid() = java.util.UUID.randomUUID.toString
}