import cats.effect._

import hello._
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.Uri
import org.scalajs.dom.HTMLElement
import calico.IOWebApp

object Client extends IOWebApp {

  def render: Resource[IO, HTMLElement] = FetchClientBuilder[IO]
    .resource
    .flatMap { fetchClient =>
      SimpleRestJsonBuilder(HelloService)
        .client(fetchClient)
        .uri(Uri.unsafeFromString(org.scalajs.dom.window.location.origin.get))
        .resource
    }
    .flatMap { client =>
      GreetingComponent.make(client)
    }

}
