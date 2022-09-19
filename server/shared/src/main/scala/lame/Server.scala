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
