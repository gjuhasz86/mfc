package com.gjuhasz.mforecast.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.annotation.tailrec
import scala.io.StdIn

object WebServer {

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher


    // `route` will be implicitly converted to `Flow` using `RouteResult.route2HandlerFlow`
    val bindingFuture = Http().bindAndHandle(Router.route, "0.0.0.0", 8080)
    println(s"Server online at http://localhost:8080/\nType 'exit' without quotes and press RETURN to stop...")

    exitLoop()

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }

  @tailrec
  def exitLoop(): Int = {
    val line = StdIn.readLine() // let it run until user presses return
    if (line == "exit") 0 else exitLoop()
  }
}