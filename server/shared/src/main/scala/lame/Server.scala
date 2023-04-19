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
import org.http4s.HttpApp
import scala.concurrent.duration._
import org.http4s.websocket.WebSocketFrame
import org.typelevel.log4cats.Logger
import fs2.concurrent.SignallingRef
import cats.effect.implicits._
import cats.kernel.Eq
import io.circe.Codec

import io.circe.syntax._
import scodec.bits.ByteVector

object Server extends IOApp.Simple {

  def run: IO[Unit] =
    SignallingRef[IO]
      .of(Payload(Map.empty))
      .toResource
      .flatMap { state =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(port"8080")
          .withHttpWebSocketApp { builder =>
            HttpRoutes
              .of[IO] { case req @ GET -> Root / "ws" =>
                val clientId = req.remote.getOrElse(sys.error("No client IP")).toString()

                builder.build(
                  send = state
                    .discrete
                    .map(_ - clientId)
                    .changes
                    .map(_.asJson.noSpaces)
                    .map(WebSocketFrame.Text(_))
                    .merge(
                      fs2
                        .Stream
                        .emit(
                          WebSocketFrame
                            .Ping(ByteVector.encodeUtf8("ping").getOrElse(sys.error("lol")))
                        )
                        .repeat
                        .metered(5.seconds)
                    ),
                  receive =
                    in =>
                      fs2
                        .Stream
                        .exec(
                          state
                            .updateAndGet(_.withNewClient(clientId))
                            .flatMap { state =>
                              IO.println(
                                s"Connected client $clientId, connected client count: ${state.data.size}"
                              )
                            }
                        ) ++
                        in
                          .collect { case WebSocketFrame.Text(data, _) => data }
                          .evalMap { data =>
                            io.circe
                              .parser
                              .decode[List[Coordinates]](data)
                              .liftTo[IO]
                              .flatMap { coords =>
                                state.update(_ + (clientId, coords))
                              }
                          }
                          .drain
                          .onFinalizeCase(ec =>
                            state.updateAndGet(_ - clientId).flatMap { state =>
                              IO.println(
                                s"Disconnected client: $clientId $ec, client count: ${state.data.size}"
                              )
                            }
                          ),
                )
              }
              .orNotFound
          }
          .build
      }
      .evalTap { server =>
        IO.println("Listening on " + server.addressIp4s)
      }
      .useForever

}
