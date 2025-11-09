package cask

/**
 * Database support for Cask using ScalaSql with named tuples (Scala 3.7+ only).
 *
 * This package provides auto-configuration of ScalaSql's DbClient from application.conf
 * and automatic transaction management via the @transactional decorator.
 *
 * Requirements:
 * - Scala 3.7+ (for named tuples / SimpleTable support)
 * - ScalaSql 0.2.3+ with scalasql-namedtuples module
 *
 * Example usage:
 * {{{
 * // application.conf
 * database {
 *   driver = "org.sqlite.JDBC"
 *   url = "jdbc:sqlite:./myapp.db"
 * }
 *
 * // Define table using SimpleTable
 * import scalasql.namedtuples.SimpleTable
 *
 * case class Todo(id: Int, checked: Boolean, text: String)
 * object Todo extends SimpleTable[Todo]
 *
 * // Use in routes with auto-configured database
 * object MyApp extends cask.MainRoutes {
 *   @cask.transactional
 *   @cask.get("/todos")
 *   def list()(using ctx: DbClient.Txn) = {
 *     ctx.run(Todo.select)
 *   }
 * }
 * }}}
 */
package object database {
  type DbClient = scalasql.DbClient
  type Txn = scalasql.DbApi.Txn
  type SimpleTable[T] = scalasql.namedtuples.SimpleTable[T]
}
