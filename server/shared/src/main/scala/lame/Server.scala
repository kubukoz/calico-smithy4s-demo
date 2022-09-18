package lame

import cats.effect.IOApp
import cats.effect.IO
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import hello._

object Server extends IOApp.Simple {

  val impl =
    new HelloService[IO] {
      def getHello(name: String): IO[GetHelloOutput] = IO(GetHelloOutput(s"Hello, $name!"))
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
          .withHttpApp(routes.orNotFound)
          .build
      }
      .useForever

}
