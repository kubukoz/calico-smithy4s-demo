package com.kubukoz.lame

import epollcat.unsafe.EpollRuntime
import cats.effect.unsafe.IORuntime

object PlatformRuntime {
  val runtime: IORuntime = EpollRuntime.global
}
