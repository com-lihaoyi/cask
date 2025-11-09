package cask.internal

import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory, ConfigException}
import scala.util.{Try, Success, Failure}
import scala.util.control.NonFatal

/**
 * Configuration loading and access with functional error handling.
 *
 * Follows Scala best practices:
 * - Either for error handling (no exceptions)
 * - ADTs for error representation
 * - Referential transparency
 * - Type safety with phantom types
 */
object Config {

  /** Configuration error ADT */
  sealed trait ConfigError {
    def message: String
  }

  object ConfigError {
    final case class Missing(key: String) extends ConfigError {
      def message = s"Configuration key '$key' is missing"
    }

    final case class InvalidType(key: String, expected: String, actual: String) extends ConfigError {
      def message = s"Configuration key '$key': expected $expected but got $actual"
    }

    final case class LoadFailure(cause: String) extends ConfigError {
      def message = s"Failed to load configuration: $cause"
    }

    final case class ParseFailure(key: String, value: String, cause: String) extends ConfigError {
      def message = s"Failed to parse '$key' value '$value': $cause"
    }
  }

  /** Environment for profile loading */
  sealed trait Environment {
    def name: String
  }

  object Environment {
    case object Development extends Environment { val name = "dev" }
    case object Test extends Environment { val name = "test" }
    case object Production extends Environment { val name = "prod" }
    final case class Custom(name: String) extends Environment

    def fromString(s: String): Environment = s.toLowerCase match {
      case "dev" | "development" => Development
      case "test" => Test
      case "prod" | "production" => Production
      case other => Custom(other)
    }

    def current: Environment =
      sys.env.get("CASK_ENV")
        .map(fromString)
        .getOrElse(Development)
  }

  /** Configuration loader with resource safety */
  final class Loader private (config: TypesafeConfig) {

    def getString(key: String): Either[ConfigError, String] =
      safeGet(key)(config.getString)

    def getInt(key: String): Either[ConfigError, Int] =
      safeGet(key)(config.getInt)

    def getBoolean(key: String): Either[ConfigError, Boolean] =
      safeGet(key)(config.getBoolean)

    def getLong(key: String): Either[ConfigError, Long] =
      safeGet(key)(config.getLong)

    def getDouble(key: String): Either[ConfigError, Double] =
      safeGet(key)(config.getDouble)

    def getStringOpt(key: String): Option[String] =
      getString(key).toOption

    def getIntOpt(key: String): Option[Int] =
      getInt(key).toOption

    def getBooleanOpt(key: String): Option[Boolean] =
      getBoolean(key).toOption

    private def safeGet[A](key: String)(f: String => A): Either[ConfigError, A] =
      Try(f(key)) match {
        case Success(value) => Right(value)
        case Failure(e: ConfigException.Missing) =>
          Left(ConfigError.Missing(key))
        case Failure(e: ConfigException.WrongType) =>
          Left(ConfigError.InvalidType(key, "unknown", e.getMessage))
        case Failure(e) =>
          Left(ConfigError.ParseFailure(key, "unknown", e.getMessage))
      }

    /** Underlying config for advanced usage */
    def underlying: TypesafeConfig = config
  }

  object Loader {

    /**
     * Load configuration with environment profile.
     *
     * Loading order (later overrides earlier):
     * 1. reference.conf (library defaults)
     * 2. application.conf (user config)
     * 3. application-{env}.conf (environment profile)
     * 4. System properties
     * 5. Environment variables
     */
    def load(env: Environment = Environment.current): Either[ConfigError, Loader] =
      Try {
        val base = ConfigFactory.load("application")
        val profile = Try(ConfigFactory.load(s"application-${env.name}"))
          .getOrElse(ConfigFactory.empty())

        profile
          .withFallback(base)
          .resolve()
      } match {
        case Success(config) => Right(new Loader(config))
        case Failure(e) => Left(ConfigError.LoadFailure(e.getMessage))
      }

    /** Load with default environment */
    def loadOrThrow(): Loader =
      load() match {
        case Right(loader) => loader
        case Left(error) => throw new RuntimeException(error.message)
      }
  }
}
