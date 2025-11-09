package cask.database

import com.typesafe.config.{Config, ConfigFactory}
import scalasql.{DbClient, DbApi}

import javax.sql.DataSource
import scala.util.Try

/**
 * Auto-configuration for ScalaSql DbClient from application.conf.
 */
object DatabaseConfig {

  def autoConfigureDbClient(): DbClient = {
    val config = Try(ConfigFactory.load()).getOrElse(ConfigFactory.empty())

    if (config.hasPath("database")) {
      val dbConfig = config.getConfig("database")
      createDbClientFromConfig(dbConfig)
    } else {
      createDefaultSqliteClient()
    }
  }

  def createDbClientFromConfig(config: Config): DbClient = {
    val driver = config.getString("driver")
    val url = config.getString("url")
    val username = if (config.hasPath("username")) Some(config.getString("username")) else None
    val password = if (config.hasPath("password")) Some(config.getString("password")) else None

    Class.forName(driver)

    val dataSource = createDataSource(driver, url, username, password)
    val dialect = detectDialect(url)
    new DbClient.DataSource(dataSource, dialect)
  }

  private def createDataSource(driver: String, url: String, username: Option[String], password: Option[String]): DataSource = {
    driver match {
      case d if d.contains("sqlite") =>
        val ds = new org.sqlite.SQLiteDataSource()
        ds.setUrl(url)
        ds

      case d if d.contains("postgresql") || d.contains("pgjdbc") =>
        throw new UnsupportedOperationException(
          "PostgreSQL requires manual DataSource configuration with connection pooling"
        )

      case d if d.contains("mysql") =>
        throw new UnsupportedOperationException(
          "MySQL requires manual DataSource configuration with connection pooling"
        )

      case _ =>
        throw new IllegalArgumentException(s"Unsupported JDBC driver: $driver")
    }
  }

  private def detectDialect(url: String): scalasql.Config = {
    url match {
      case u if u.startsWith("jdbc:sqlite:") =>
        new scalasql.Config {
          override def sqliteDialect: Boolean = true
        }

      case u if u.startsWith("jdbc:postgresql:") =>
        new scalasql.Config {
          override def postgresDialect: Boolean = true
        }

      case u if u.startsWith("jdbc:mysql:") =>
        new scalasql.Config {
          override def mysqlDialect: Boolean = true
        }

      case u if u.startsWith("jdbc:h2:") =>
        new scalasql.Config {
          override def h2Dialect: Boolean = true
        }

      case _ =>
        new scalasql.Config {}
    }
  }

  private def createDefaultSqliteClient(): DbClient = {
    val ds = new org.sqlite.SQLiteDataSource()
    ds.setUrl("jdbc:sqlite::memory:")
    new DbClient.DataSource(
      ds,
      new scalasql.Config {
        override def sqliteDialect: Boolean = true
      }
    )
  }
}
