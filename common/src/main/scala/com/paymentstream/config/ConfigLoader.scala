package com.paymentstream.config

import java.nio.file.{Path, Paths}

import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._
import zio.{TaskLayer, ZIO, ZLayer}

object ConfigLoader {

  // Путь к HOCON
  private def configPath: Path = {
    sys.props.get("config.file").orElse(sys.env.get("CONFIG_FILE")).map(Paths.get(_))
      .getOrElse(Paths.get("config", "application.conf"))
  }

  def load(): Either[ConfigReaderFailures, AppConfig] = ConfigSource.file(configPath)
    .load[AppConfig]

  def loadOrThrow(): AppConfig = load()
    .fold(failures => throw new IllegalArgumentException(failures.prettyPrint()), identity)

  val layer: TaskLayer[AppConfig] = ZLayer.fromZIO(ZIO.fromEither(load()).mapError(failures =>
    new IllegalArgumentException(failures.prettyPrint())
  ))
}
