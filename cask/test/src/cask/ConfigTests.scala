package test.cask

import utest._

object ConfigTests extends TestSuite {

  val tests = Tests {
    test("ADT error types") {
      val missing = cask.Config.ConfigError.Missing("test.key")
      assert(missing.message.contains("missing"))

      val invalidType = cask.Config.ConfigError.InvalidType("test.key", "String", "Int")
      assert(invalidType.message.contains("expected"))
    }

    test("Environment from string") {
      import cask.Config.Environment._

      assert(fromString("dev") == Development)
      assert(fromString("development") == Development)
      assert(fromString("test") == Test)
      assert(fromString("prod") == Production)
      assert(fromString("production") == Production)
      assert(fromString("staging").isInstanceOf[Custom])
    }

    test("Config loader Either pattern") {
      val result = cask.Config.getString("nonexistent.key")
      assert(result.isLeft)

      result match {
        case Left(error) => assert(error.message.contains("missing"))
        case Right(_) => assert(false)
      }
    }

    test("Optional config accessors") {
      val opt = cask.Config.getStringOpt("nonexistent.key")
      assert(opt.isEmpty)
    }

    test("Type-safe accessors with different types") {
      // These will work if application.conf exists with proper values
      // Test that methods exist and return correct types
      locally {
        val _: Either[cask.Config.ConfigError, String] = cask.Config.getString("any.key")
      }
      locally {
        val _: Either[cask.Config.ConfigError, Int] = cask.Config.getInt("any.key")
      }
      locally {
        val _: Either[cask.Config.ConfigError, Boolean] = cask.Config.getBoolean("any.key")
      }
      locally {
        val _: Either[cask.Config.ConfigError, Long] = cask.Config.getLong("any.key")
      }
      locally {
        val _: Either[cask.Config.ConfigError, Double] = cask.Config.getDouble("any.key")
      }
    }

    test("Environment detection from CASK_ENV") {
      val env = cask.Config.Environment.current
      // Should default to Development if CASK_ENV not set
      assert(env.isInstanceOf[cask.Config.Environment])
    }

    test("Config error pattern matching") {
      import cask.Config.ConfigError._

      val errors: Seq[cask.Config.ConfigError] = Seq(
        Missing("key1"),
        InvalidType("key2", "String", "Int"),
        LoadFailure("cause"),
        ParseFailure("key3", "value", "cause")
      )

      errors.foreach { error =>
        error match {
          case Missing(key) => assert(key.nonEmpty)
          case InvalidType(key, expected, actual) =>
            assert(key.nonEmpty && expected.nonEmpty && actual.nonEmpty)
          case LoadFailure(cause) => assert(cause.nonEmpty)
          case ParseFailure(key, value, cause) =>
            assert(key.nonEmpty && cause.nonEmpty)
        }
      }
    }
  }
}
