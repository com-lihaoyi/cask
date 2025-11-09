package cask

/**
 * Database support for Cask using ScalaSql with named tuples (Scala 3.7+ only).
 *
 * To use database features with Scala 3.7+, add these dependencies to your project:
 * {{{
 * mvn"com.lihaoyi::scalasql-namedtuples:0.2.3"
 * mvn"org.xerial:sqlite-jdbc:3.42.0.0" // or your preferred JDBC driver
 * }}}
 *
 * Then import the simple API in your routes:
 * {{{
 * import scalasql.simple.{*, given}
 * import SqliteDialect._ // or your database dialect
 *
 * given dbClient: scalasql.core.DbClient = new DbClient.DataSource(dataSource, config = new {})
 *
 * @cask.database.transactional[scalasql.core.DbClient]
 * @cask.get("/todos")
 * def list()(using ctx: scalasql.core.DbApi.Txn) = {
 *   ctx.run(Todo.select)
 * }
 * }}}
 */
package object database {}
