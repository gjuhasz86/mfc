package com.gjuhasz.mforecast.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.gjuhasz.mforecast.lib.Mfc
import com.gjuhasz.mforecast.shared.model._
import example.shared.Foo
import de.heikoseeberger.akkahttpcirce._ // keep
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe._ // keep
import io.circe.generic.auto._
import io.circe.parser._ // keep
import com.gjuhasz.mforecast.shared.utils.CirceInstances._ // keep
import io.circe.syntax._ // keep

object Router {

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
      get0("list") {
        println("Requesting list")
        val foo = Foo("bar", List(1, 2, 3))
        complete(foo)
      }
      post0("plan") {
        entity(as[MfcArgs]) { mfcArgs =>
          import mfcArgs._
          val res = Mfc.plan(start, cashflows, defaultAccount, allocated.withDefaultValue(0))
          complete(res)
        }
      }
    }
}