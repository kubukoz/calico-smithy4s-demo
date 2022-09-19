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

package lame

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.UUIDGen
import cats.implicits._
import com.comcast.ip4s._
import hello._
import org.http4s.HttpRoutes
import org.http4s.StaticFile
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import smithy4s.http4s.SimpleRestJsonBuilder

object Server extends IOApp.Simple {

  val impl =
    new HelloService[IO] {

      def getHello(name: String): IO[GetHelloOutput] = UUIDGen.randomUUID[IO].flatMap { uuid =>
        IO(GetHelloOutput(s"Hello, $name! ID: " + uuid))
      }

    }

  val staticRoutes: HttpRoutes[IO] = HttpRoutes.of {
    case req @ GET -> ((Root / "index.html") | Root) =>
      StaticFile
        .fromResource("frontend/index.html", Some(req))
        .getOrElseF(InternalServerError())

    case req @ GET -> path if path.startsWith(Root / "assets") =>
      StaticFile
        .fromResource("frontend/" + path.renderString, Some(req))
        .getOrElseF(InternalServerError())
  }

  def run: IO[Unit] =
    SimpleRestJsonBuilder
      .routes(impl)
      .resource
      .flatMap { routes =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp((routes <+> staticRoutes).orNotFound)
          .build
      }
      .useForever

}
