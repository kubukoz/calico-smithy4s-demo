package com.kubukoz.lame

import cats.effect.kernel.Resource
import smithy4s.Service
import cats.instances.int
import cats.effect.IO
import smithy4s.schema.SchemaVisitor
import smithy.api.Http
import smithy4s.http.HttpEndpoint
import smithy4s.http.PathSegment.LabelSegment
import smithy4s.http.PathSegment.GreedySegment
import smithy4s.http.PathSegment.StaticSegment
import cats.implicits._
import smithy4s.http.Metadata
import smithy4s.http.Metadata.PartialDecoder
import smithy4s.http.Metadata.TotalDecoder
import smithy4s.kinds.PolyFunction5
import calico.router.Router
import org.http4s.Uri
import org.http4s.Uri.Path
import calico.router.*
import org.http4s.Uri.Path.Segment

import calico.*
import calico.html.*
import calico.html.io.given
import calico.html.io.*
import fs2.dom.*
import calico.syntax.*

object FrontRouteInterpreter {

  type Paged[_] = Resource[IO, HtmlElement[IO]]

  type ClientCall[_] = IO[Unit]

  def client[Alg[_[_, _, _, _, _]]](
    service: Service[Alg],
    router: Router[IO],
  ): smithy4s.kinds.FunctorAlgebra[Alg, ClientCall] = {

    val fk =
      new PolyFunction5[service.Operation, [_, _, _, _, _] =>> IO[Unit]] {
        def apply[I, E, O, SI, SO](
          fa: service.Operation[I, E, O, SI, SO]
        ): IO[Unit] = {
          val (in, e) = service.endpoint(fa)

          val httpEndpoint = HttpEndpoint.cast(e).toTry.get
          val p = httpEndpoint.path(in)
          // todo: later
          // val staticQP = httpEndpoint.staticQueryParams
          import org.http4s.implicits._
          val uri = Uri(
            path = Uri.Path(
              segments = p.map(Segment(_)).toVector,
              absolute = true,
              endsWithSlash = true,
            )
          )
          println("navigating to uri: " + uri)
          router.navigate(
            uri
          )
        }
      }

    service.fromPolyFunction(fk)
  }

  def router[Alg[_[_, _, _, _, _]]](
    impl: smithy4s.kinds.FunctorAlgebra[Alg, Paged]
  )(
    using service: Service[Alg]
  ): IO[Routes[IO]] = {

    val interp = service.toPolyFunction(impl)

    def handleEndpoint[I, E, O, SI, SO](e: service.Endpoint[I, E, O, SI, SO]): IO[Routes[IO]] = {
      val httpEndpoint = HttpEndpoint.cast(e).toTry.get

      val cache = PartialDecoder.createCache()
      val decoder: TotalDecoder[I] = PartialDecoder.fromSchema(e.input).total.get

      object validInput {
        def unapply(uri: Uri): Option[Map[String, String]] = httpEndpoint.matches(
          uri.path.segments.map(_.decoded()).toArray
        )
      }
      Routes.one[IO] { case uri @ validInput(value) =>
        val meta = Metadata.apply(
          path = value,
          query = uri.multiParams,
          headers = Map.empty,
          statusCode = None,
        )

        decoder.decode(meta) match {
          // basically bad request. should be handled somehow (customizable)
          case Left(e)      => sys.error("invalid: " + e)
          case Right(input) => input

        }
      }(sig => div(sig.map(in => interp(e.wrap(in)))))
    }

    service.endpoints.traverse(handleEndpoint(_)).map(_.reduceLeft(_ |+| _))

  }

}
