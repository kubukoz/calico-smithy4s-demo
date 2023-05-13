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
import calico.frp.given
import calico.html.io.*
import calico.html.io.given
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.*
import fs2.dom.*
import hello.HelloService

object GreetingComponent {

  case class State(
    name: String,
    greeting: Option[String],
    greetingHistory: List[String],
  )

  def make(
    client: HelloService[IO]
  ): Resource[IO, HtmlElement[IO]] = SignallingRef[IO, State](
    State(
      name = "user",
      greeting = None,
      greetingHistory = Nil,
    )
  )
    .toResource
    .flatMap { state =>
      val getName = state.map(_.name)
      val getGreeting = state.map(_.greeting)
      val getGreetingHistory = state.map(_.greetingHistory)

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
    name: Signal[IO, String],
    handleInput: String => IO[Unit],
  ): Resource[IO, HtmlElement[IO]] = div(
    label("Your name"),
    input.withSelf { self =>
      (
        placeholder := "Enter your name here",
        value <-- name,
        onInput --> (_.foreach(_ => self.value.get.flatMap(handleInput))),
      )
    },
  )

}

object GreetingButton {

  def component(
    name: Signal[IO, String],
    handleClick: IO[Unit],
  ): Resource[IO, HtmlElement[IO]] = button(
    "Get greeting for ",
    name,
    onClick --> (_.foreach(_ => handleClick)),
  )

}

object GreetingViewer {

  def component(
    greeting: Signal[IO, Option[String]],
    greetingHistory: Signal[IO, List[String]],
  ): Resource[IO, HtmlElement[IO]] = div(
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
