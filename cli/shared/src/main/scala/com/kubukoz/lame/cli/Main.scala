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
import smithy4s.GenLift
import cats.effect.MonadCancelThrow
import smithy4s.Service
import smithy4s.Transformation

object Main extends CommandIOApp("calico-demo", "Calico demo", true, "0.1.0") {
  override protected val runtime: IORuntime = PlatformRuntime.runtime

  def unliftService[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_]: MonadCancelThrow](
    algRes: Resource[F, Alg[GenLift[F]#λ]]
  )(
    implicit service: Service[Alg, Op]
  ): Alg[GenLift[F]#λ] = service.transform[GenLift[F]#λ](new Transformation[Op, GenLift[F]#λ] {

    def apply[A, B, C, D, E](
      op: Op[A, B, C, D, E]
    ): F[C] = algRes.use(service.asTransformation[GenLift[F]#λ](_)(op))

  })

  def makeClient(
    url: Uri
  ): Resource[IO, HelloService[IO]] = EmberClientBuilder.default[IO].build.flatMap {
    SimpleRestJsonBuilder(HelloServiceGen).client(_).uri(url).resource
  }

  def main: Opts[IO[ExitCode]] = Smithy4sCli(
    Opts
      .option[String]("base-url", "Base URL")
      .map(Uri.unsafeFromString(_))
      .withDefault(uri"http://localhost:8080")
      .map(url => Entrypoint(unliftService(makeClient(url)), PrinterApi.std[IO])),
    HelloServiceGen,
  ).opts.map(_.as(ExitCode.Success))

}
