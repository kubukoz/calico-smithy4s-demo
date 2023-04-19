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
import org.http4s.dom.WebSocketClient
import org.http4s.client.websocket.WSRequest
import calico.html.io._
import calico.html.io.given
import calico.frp.given
import org.http4s.client.websocket.WSFrame.Text
import cats.implicits._
import scala.scalajs.js.JavaScriptException
import scala.scalajs.js.JSON
import scala.concurrent.duration._
import cats.effect.std.UUIDGen
import lame.Coordinates
import java.util.UUID
import io.circe.syntax._
import org.http4s.websocket.WebSocketFrame
import org.http4s.client.websocket.WSDataFrame
import org.http4s.client.websocket.WSFrame
import lame.Payload
import fs2.concurrent.SignallingRef
import cats.kernel.Monoid
import scodec.bits.ByteVector

object Client extends IOWebApp {

  def render: Resource[IO, HtmlElement[cats.effect.IO]] = WebSocketClient[IO]
    .connectHighLevel(
      WSRequest(Uri.unsafeFromString(s"ws://192.168.0.101:8080/ws"))
    )
    .flatMap { wss =>
      fs2
        .dom
        .events[IO, org.scalajs.dom.MouseEvent](org.scalajs.dom.window, "mousemove")
        .map { me =>
          Coordinates(me.clientX.toInt, me.clientY.toInt) :: Nil
        }
        .merge(
          List("touchmove", "touchstart", "touchend")
            .map {
              fs2
                .dom
                .events[IO, org.scalajs.dom.TouchEvent](org.scalajs.dom.window, _)
            }
            .foldLeft[fs2.Stream[IO, org.scalajs.dom.TouchEvent]](fs2.Stream.empty)(_ merge _)
            .map { te =>
              te.targetTouches
                .map { t =>
                  Coordinates(t.clientX.toInt, t.clientY.toInt)
                }
                .toList
            }
            .scan1 {
              case (previous, Nil) => previous.take(1)
              case (_, next)       => next
            }
        )
        .holdResource(List(Coordinates(0, 0)))
        .flatMap { currentPosSig =>

          val sendPos =
            currentPosSig
              .discrete
              .changes
              .map((_: List[Coordinates]).asJson.noSpaces)
              .map(WSFrame.Text(_))
              .through(wss.sendPipe)
              .compile
              .drain
              .background
              .void

          div(
            "Positions:",
            sendPos,
            wss
              .receiveStream
              .collect { case Text(data, _) => io.circe.parser.decode[Payload](data) }
              .evalMapFilter {
                case Right(payload) => IO.pure(Some(payload))
                case Left(e)        => IO.consoleForIO.printStackTrace(e).as(None)
              }
              .holdResource(Monoid.empty[Payload])
              .flatMap { sig =>
                (sig, currentPosSig)
                  .mapN { (payload, myCoords) =>
                    payload + ("You", myCoords)
                  }
                  // might not be necessary after https://github.com/typelevel/fs2/pull/3206
                  // and https://github.com/armanbilge/calico/pull/229/
                  .discrete
                  .hold1Resource
              }
              .map { itemsSig =>
                ul(
                  children[String] { case s"$k-$i" =>
                    val itemSig = itemsSig.map(_.data.get(k).flatMap(_.lift(i.toInt)))

                    li(
                      styleAttr <-- itemSig.map {
                        case Some(Coordinates(x, y)) =>
                          s"position: absolute; top: ${y}px; left: ${x}px;"
                        case _ => "display: none;"
                      },
                      itemSig.map(item =>
                        s"$k($i): ${item
                            .map { case Coordinates(x, y) => s"($x, $y)" }
                            .getOrElse("<position unknown>")}"
                      ),
                    )
                  } <-- itemsSig.map(_.data.toList.sortBy(_._1).flatMap { case (k, values) =>
                    values.indices.map(i => s"$k-$i")
                  })
                )
              },
          )
        }
    }

}
