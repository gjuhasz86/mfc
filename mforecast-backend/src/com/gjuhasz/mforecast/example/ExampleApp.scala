package com.gjuhasz.mforecast.example


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.{ActorMaterializer, Materializer}
import com.gjuhasz.mforecast.shared.model.{Category, MfcArgs}
import io.circe.syntax._
import com.gjuhasz.mforecast.shared.utils.CirceInstances._
import scala.io.StdIn

object ExampleApp {

  private final case class Foo(bar: String)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()

    Http().bindAndHandle(route, "127.0.0.1", 8000)

    StdIn.readLine("Hit ENTER to exit")
    system.terminate()
  }



  private def route(implicit mat: Materializer) = {
    import Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import io.circe.generic.auto._


    val m: Map[Category, Int] = ???
    m.asJson

    pathSingleSlash {
      post {
        entity(as[MfcArgs]) { foo =>
          complete {
            foo
          }
        }
      }
    }
  }
}