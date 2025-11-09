package cask

import cask.internal.{Config => InternalConfig}

/**
 * Global configuration access point.
 *
 * Auto-loads configuration at startup from:
 * - application.conf
 * - application-{CASK_ENV}.conf
 * - System properties
 * - Environment variables
 *
 * Example:
 * {{{
 * // application.conf
 * app {
 *   name = "my-app"
 *   port = 8080
 *   database.url = ${?DATABASE_URL}
 * }
 *
 * // Usage
 * val name = cask.Config.getString("app.name")
 * val port = cask.Config.getInt("app.port")
 * }}}
 */
object Config {

  type ConfigError = InternalConfig.ConfigError
  val ConfigError = InternalConfig.ConfigError

  type Environment = InternalConfig.Environment
  val Environment = InternalConfig.Environment

  /** Lazily loaded configuration */
  private lazy val loader: InternalConfig.Loader =
    InternalConfig.Loader.loadOrThrow()

  /** Get string configuration value */
  def getString(key: String): Either[ConfigError, String] =
    loader.getString(key)

  /** Get int configuration value */
  def getInt(key: String): Either[ConfigError, Int] =
    loader.getInt(key)

  /** Get boolean configuration value */
  def getBoolean(key: String): Either[ConfigError, Boolean] =
    loader.getBoolean(key)

  /** Get long configuration value */
  def getLong(key: String): Either[ConfigError, Long] =
    loader.getLong(key)

  /** Get double configuration value */
  def getDouble(key: String): Either[ConfigError, Double] =
    loader.getDouble(key)

  /** Get optional string */
  def getStringOpt(key: String): Option[String] =
    loader.getStringOpt(key)

  /** Get optional int */
  def getIntOpt(key: String): Option[Int] =
    loader.getIntOpt(key)

  /** Get optional boolean */
  def getBooleanOpt(key: String): Option[Boolean] =
    loader.getBooleanOpt(key)

  /** Get string or throw exception */
  def getStringOrThrow(key: String): String =
    getString(key) match {
      case Right(value) => value
      case Left(error) => throw new RuntimeException(error.message)
    }

  /** Get int or throw exception */
  def getIntOrThrow(key: String): Int =
    getInt(key) match {
      case Right(value) => value
      case Left(error) => throw new RuntimeException(error.message)
    }

  /** Get boolean or throw exception */
  def getBooleanOrThrow(key: String): Boolean =
    getBoolean(key) match {
      case Right(value) => value
      case Left(error) => throw new RuntimeException(error.message)
    }

  /** Access underlying Typesafe Config for advanced usage */
  def underlying: com.typesafe.config.Config =
    loader.underlying

  /** Reload configuration (useful for testing) */
  private[cask] def reload(): Unit = {
    // Force re-evaluation of lazy val
    val _ = InternalConfig.Loader.loadOrThrow()
  }
}
