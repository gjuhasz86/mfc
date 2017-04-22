package example.shared

import scala.scalajs.js.annotation._

@JSExportTopLevel("api.HelloWorld")
object HelloWorld {
  @JSExport
  def sayHello(): String = {
    println("Hello world something new!")
    "result"
  }
}