package com.kubukoz.lame

import cats.effect.unsafe.IORuntime

object PlatformRuntime {
  val runtime: IORuntime = IORuntime.global
}
