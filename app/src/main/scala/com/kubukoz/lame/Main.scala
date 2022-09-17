package com.kubukoz.lame

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Main extends App {
  import org.scalajs.dom
  import com.raquo.laminar.api.L._

  val nameVar = Var(initial = "world")

  val rootElement = div(
    img(src := "resources/star.png"),
    label("Your name"),
    input(
      onMountFocus,
      placeholder := "Enter your name here",
      onInput.mapToValue --> nameVar,
    ),
    span(
      "Hello, ",
      child.text <-- nameVar.signal.map(_.toUpperCase).map {
        typings.leftPad.mod(_, 20, "!")
      },
    ),
  )

  // In most other examples, containerNode will be set to this behind the scenes
  val containerNode = dom.document.querySelector("#app")

  render(containerNode, rootElement)
}
