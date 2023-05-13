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

import cats.effect._

import hello._
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.Uri
import calico.IOWebApp
import fs2.dom.HtmlElement
import fs2.dom.Window
import cats.implicits._

object Client extends IOWebApp {

  def render: Resource[IO, HtmlElement[cats.effect.IO]] = FetchClientBuilder[IO]
    .resource
    .flatMap { fetchClient =>
      Window[IO].location.origin.flatMap(Uri.fromString(_).liftTo[IO]).toResource.flatMap {
        SimpleRestJsonBuilder(HelloService)
          .client(fetchClient)
          .uri(_)
          .resource
      }
    }
    .flatMap { client =>
      GreetingComponent.make(client)
    }

}
