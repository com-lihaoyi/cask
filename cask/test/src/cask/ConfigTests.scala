package test.cask

import cask.Config
import cask.Config.ConfigError
import cask.Config.Environment
import utest._

object ConfigTests extends TestSuite {

  val tests = Tests {
    test("ConfigError ADT") {
      test("Missing") {
        val missing = ConfigError.Missing("test.key")
        assert(missing.message.contains("missing"))
        assert(missing.message.contains("test.key"))
      }

      test("InvalidType") {
        val invalidType = ConfigError.InvalidType("test.key", "String", "Int")
        assert(invalidType.message.contains("expected"))
        assert(invalidType.message.contains("String"))
        assert(invalidType.message.contains("Int"))
      }

      test("LoadFailure") {
        val loadFailure = ConfigError.LoadFailure("file not found")
        assert(loadFailure.message.contains("Failed to load"))
        assert(loadFailure.message.contains("file not found"))
      }

      test("ParseFailure") {
        val parseFailure = ConfigError.ParseFailure("test.key", "invalid", "syntax error")
        assert(parseFailure.message.contains("test.key"))
        assert(parseFailure.message.contains("invalid"))
        assert(parseFailure.message.contains("syntax error"))
      }
    }

    test("Environment") {
      test("fromString conversions") {
        assert(Environment.fromString("dev") == Environment.Development)
        assert(Environment.fromString("development") == Environment.Development)
        assert(Environment.fromString("test") == Environment.Test)
        assert(Environment.fromString("prod") == Environment.Production)
        assert(Environment.fromString("production") == Environment.Production)

        val custom = Environment.fromString("staging")
        assert(custom.isInstanceOf[Environment.Custom])
        assert(custom.name == "staging")
      }

      test("current environment") {
        val env = Environment.current
        assert(env.isInstanceOf[Environment])
        assert(env.name.nonEmpty)
      }

      test("environment names") {
        assert(Environment.Development.name == "dev")
        assert(Environment.Test.name == "test")
        assert(Environment.Production.name == "prod")
        assert(Environment.Custom("staging").name == "staging")
      }
    }

    test("Config getString") {
      test("missing key returns Left") {
        val result = Config.getString("nonexistent.key")
        assert(result.isLeft)

        result match {
          case Left(error) =>
            assert(error.isInstanceOf[ConfigError.Missing])
            assert(error.message.contains("missing"))
          case Right(_) => assert(false)
        }
      }

      test("pattern matching on Either") {
        Config.getString("nonexistent") match {
          case Left(_: ConfigError.Missing) => // Expected
          case _ => assert(false)
        }
      }
    }

    test("Config optional accessors") {
      test("getStringOpt returns None for missing") {
        val opt = Config.getStringOpt("nonexistent.key")
        assert(opt.isEmpty)
      }

      test("getIntOpt returns None for missing") {
        val opt = Config.getIntOpt("nonexistent.key")
        assert(opt.isEmpty)
      }

      test("getBooleanOpt returns None for missing") {
        val opt = Config.getBooleanOpt("nonexistent.key")
        assert(opt.isEmpty)
      }
    }

    test("Config type-safe accessors") {
      test("getString returns Either") {
        val _: Either[ConfigError, String] = Config.getString("any.key")
      }

      test("getInt returns Either") {
        val _: Either[ConfigError, Int] = Config.getInt("any.key")
      }

      test("getBoolean returns Either") {
        val _: Either[ConfigError, Boolean] = Config.getBoolean("any.key")
      }

      test("getLong returns Either") {
        val _: Either[ConfigError, Long] = Config.getLong("any.key")
      }

      test("getDouble returns Either") {
        val _: Either[ConfigError, Double] = Config.getDouble("any.key")
      }
    }

    test("Config throw accessors") {
      test("getStringOrThrow throws on missing") {
        try {
          Config.getStringOrThrow("nonexistent.key")
          assert(false)
        } catch {
          case e: RuntimeException => assert(e.getMessage.contains("missing"))
        }
      }

      test("getIntOrThrow throws on missing") {
        try {
          Config.getIntOrThrow("nonexistent.key")
          assert(false)
        } catch {
          case e: RuntimeException => assert(e.getMessage.contains("missing"))
        }
      }

      test("getBooleanOrThrow throws on missing") {
        try {
          Config.getBooleanOrThrow("nonexistent.key")
          assert(false)
        } catch {
          case e: RuntimeException => assert(e.getMessage.contains("missing"))
        }
      }
    }

    test("Config underlying Typesafe Config access") {
      val underlying = Config.underlying
      assert(underlying != null)
      assert(underlying.isInstanceOf[com.typesafe.config.Config])
    }

    test("ConfigError pattern matching") {
      val errors: Seq[ConfigError] = Seq(
        ConfigError.Missing("key1"),
        ConfigError.InvalidType("key2", "String", "Int"),
        ConfigError.LoadFailure("cause"),
        ConfigError.ParseFailure("key3", "value", "cause")
      )

      errors.foreach { error =>
        error match {
          case ConfigError.Missing(key) => assert(key.nonEmpty)
          case ConfigError.InvalidType(key, expected, actual) =>
            assert(key.nonEmpty && expected.nonEmpty && actual.nonEmpty)
          case ConfigError.LoadFailure(cause) => assert(cause.nonEmpty)
          case ConfigError.ParseFailure(key, value, cause) =>
            assert(key.nonEmpty && cause.nonEmpty)
        }
      }
    }

    test("Config is process-scoped and immutable") {
      // Multiple accesses return same underlying config
      val config1 = Config.underlying
      val config2 = Config.underlying
      assert(config1 eq config2)
    }
  }
}
