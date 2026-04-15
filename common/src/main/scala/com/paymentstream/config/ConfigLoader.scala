package com.paymentstream.config

import java.nio.file.{Path, Paths}

import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

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
}
