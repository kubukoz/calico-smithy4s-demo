package com.kubukoz.lame.cli

import com.monovore.decline.effect.CommandIOApp
import cats.effect.ExitCode
import cats.effect.IO
import com.monovore.decline.Opts
import smithy4s.decline.Smithy4sCli
import smithy4s.decline.Entrypoint
import smithy4s.http4s.SimpleRestJsonBuilder
import hello._
import org.http4s.client.Client
import smithy4s.decline.util.PrinterApi
import org.http4s.ember.client.EmberClientBuilder
import cats.effect.kernel.Resource
import org.http4s.Uri
import org.http4s.implicits._
import cats.effect.unsafe.IORuntime
import com.kubukoz.lame.PlatformRuntime
import cats.effect.MonadCancelThrow
import smithy4s.Service
import smithy4s.Transformation
import smithy4s.kinds._

object Main extends CommandIOApp("calico-demo", "Calico demo", true, "0.1.0") {
  override protected val runtime: IORuntime = PlatformRuntime.runtime

  def unliftService[Alg[_[_, _, _, _, _]], F[_]: MonadCancelThrow](
    algRes: Resource[F, FunctorAlgebra[Alg, F]]
  )(
    implicit service: Service[Alg]
  ): FunctorAlgebra[Alg, F] = service.fromPolyFunction(
    new PolyFunction5[service.Operation, Kind1[F]#toKind5] {

      def apply[I, E, O, SI, SO](
        op: service.Operation[I, E, O, SI, SO]
      ): F[O] = algRes.use(service.toPolyFunction(_)(op))

    }
  )

  def makeClient(
    url: Uri
  ): Resource[IO, HelloService[IO]] = EmberClientBuilder.default[IO].build.flatMap {
    SimpleRestJsonBuilder(HelloService).client(_).uri(url).resource
  }

  val mainOpts: Opts[Entrypoint[HelloServiceGen, IO]] = Opts
    .option[String]("base-url", "Base URL")
    .map(Uri.unsafeFromString(_))
    .withDefault(uri"http://localhost:8080")
    .map(url => Entrypoint(unliftService(makeClient(url)), PrinterApi.std[IO]))

  def main: Opts[IO[ExitCode]] = Smithy4sCli(
    mainOpts,
    HelloService,
  ).opts.map(_.as(ExitCode.Success))

}
