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

import calico.*
import calico.html.*
import calico.html.io.given
import calico.html.io.*
import fs2.dom.*
import calico.syntax.*
import com.kubukoz.lame.FrontRouteInterpreter
import frontsmith.MyRoutes
import com.kubukoz.lame.FrontRouteInterpreter.Paged
import cats.effect.implicits.*
import calico.router.Router
import org.http4s.implicits.*
import cats.implicits.*

object Client extends IOWebApp {

  val handler =
    new MyRoutes[Paged] {
      override def home() = div("home page")
      override def profile(id: String) = div(s"profile page: $id")
    }

  def render: Resource[IO, HtmlElement[cats.effect.IO]] = Router(window).toResource.flatMap {
    router =>
      val navigate = FrontRouteInterpreter.navigator(MyRoutes, router)

      div(
        "Navigation: ",
        a(
          onClick --> (_.foreach(_ => navigate.home())),
          b("HOME"),
        ),
        " | ",
        a(
          onClick --> (_.foreach(_ => navigate.profile(id = "50"))),
          b("PROFILE"),
        ),
        p("content:"),
        router.dispatch(FrontRouteInterpreter.router(handler)),
      )
  }

}
