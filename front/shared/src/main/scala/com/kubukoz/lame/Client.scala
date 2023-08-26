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

import calico.*
import calico.frp.given
import calico.html.io.*
import calico.html.io.given
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.*
import fs2.dom.*
import hello.HelloService
import smithy4s.schema.Schema
import cats.kernel.Hash

object Client extends IOWebApp {

  private val capi = smithy4s.http.json.codecs()
  private val documentCodec = capi.compileCodec(Schema.document)

  given Hash[Issue] = Hash.fromUniversalHashCode

  def issueTable(
    endpoint: IO[List[Issue]]
  ) = SignallingRef[IO]
    .of(List.empty[Issue])
    .toResource
    .flatTap { sig =>
      endpoint.flatMap(sig.set).background
    }
    .flatMap { results =>
      table(
        thead(
          th("Key"),
          th("Summary"),
          th("Status"),
        ),
        tbody(
          children <-- results
            .nested
            .map { issue =>
              tr(
                td(issue.key),
                td(
                  a(
                    href := s"${Secret.baseUrlNoSlash}/browse/${issue.key}",
                    issue.fields.summary,
                    title := issue.fields.description.getOrElse(""),
                  )
                ),
                td(issue.fields.status.map(_.name).getOrElse("")),
              )
            }
            .value
        ),
      )
    }

  def render: Resource[IO, HtmlElement[cats.effect.IO]] = FetchClientBuilder[IO]
    .resource
    .flatMap { fetchClient =>
      //
      Window[IO].location.origin.flatMap(Uri.fromString(_).liftTo[IO]).toResource.flatMap {
        SimpleRestJsonBuilder(JiraService)
          .client(fetchClient)
          .uri(_)
          .resource
      }
    }
    .flatMap { client =>
      htmlRootTag(
        table(
          tr(
            td(
              h1(
                "SPRINT:"
              ),
              issueTable(
                client
                  .getIssuesForSprint(
                    sprintId = 10211,
                    jql =
                      s"""project = "${Secret.projectName}" AND "Team(s)" in ("${Secret.teamName}") AND status != Done ORDER BY Rank ASC""".some,
                    fields = "summary,description,status",
                  )
                  .map(_.issues)
              ),
            )
          )
        )
      )
    }

}
