/*
 * Copyright 2022 Jakub Kozłowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import calico.*

import calico.dsl.Dsl
import calico.dsl.io.*
import calico.syntax.*
import calico.unsafe.*
import cats.effect._
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import fs2.*
import fs2.concurrent.SignallingRef
import hello._
import monocle.Lens
import org.http4s.Uri
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._
import org.scalajs.dom.HTMLDivElement
import org.scalajs.dom.HTMLElement
import org.scalajs.dom.HTMLStyleElement
import org.scalajs.dom.html.Button
import org.scalajs.dom.html.Div
import smithy4s.http4s.SimpleRestJsonBuilder

object GreetingComponent {

  case class State(
    name: String,
    greeting: Option[String],
    greetingHistory: List[String],
  )

  def make(
    client: HelloService[IO]
  ): Resource[IO, HTMLDivElement] = SignallingRef[IO, State](
    State(
      name = "user",
      greeting = None,
      greetingHistory = Nil,
    )
  )
    .toResource
    .flatMap { state =>
      val getName = state.discrete.map(_.name).changes
      val getGreeting = state.discrete.map(_.greeting).changes
      val getGreetingHistory = state.discrete.map(_.greetingHistory).changes

      val handleNameChange =
        (name: String) =>
          state.update(s =>
            s.copy(
              name = name,
              greeting = none,
              greetingHistory = s
                .greetingHistory
                .appendedAll(s.greeting.toList),
            )
          )

      val handleGreetingRequest = state.get.flatMap { s =>
        client
          .getHello(s.name)
          .flatMap { out =>
            state.update(
              _.copy(
                greeting = out.greeting.some,
                greetingHistory = s.greetingHistory.appendedAll(s.greeting),
              )
            )
          }
      }

      div(
        NameInput.component(
          getName,
          handleNameChange,
        ),
        GreetingButton.component(
          getName,
          handleClick = handleGreetingRequest,
        ),
        GreetingViewer.component(
          greeting = getGreeting,
          greetingHistory = getGreetingHistory,
        ),
      )
    }

}

object NameInput {

  def component(
    name: Stream[IO, String],
    handleInput: String => IO[Unit],
  ): Resource[IO, Div] = div(
    label("Your name"),
    input(
      placeholder := "Enter your name here",
      value <-- name,
      onInput --> (_.mapToTargetValue.foreach(handleInput(_))),
    ),
  )

}

object GreetingButton {

  def component(
    name: Stream[IO, String],
    handleClick: IO[Unit],
  ): Resource[IO, Button] = button(
    "Get greeting for ",
    name,
    onClick --> (_.foreach(_ => handleClick)),
  )

}

object GreetingViewer {

  import scala.concurrent.duration.DurationInt

  def component(
    greeting: Stream[IO, Option[String]],
    greetingHistory: Stream[IO, List[String]],
  ): Resource[IO, Div] = div(
    "Greeting history:",
    div(
      children <-- greetingHistory.map {
        _.zipWithIndex.map { (greeting, i) =>
          div(
            s"${i + 1}: $greeting"
          )
        }
      }
    ),
    div(
      children <-- greeting.map {
        case Some(v) =>
          h2(
            "Latest greeting: ",
            v,
          ) :: Nil
        case None => Nil
      }
    ),
  )

}
