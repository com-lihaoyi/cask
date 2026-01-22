package cask.database

import cask.model.Response.Raw
import cask.router.{RawDecorator, Result}
import cask.model.{Request, Response}
import scalasql.core.DbClient

/**
 * Decorator that wraps route execution in a database transaction.
 *
 * Requires Scala 3.7+ and scalasql-namedtuples dependency in your project.
 * Automatically commits on success and rolls back on exceptions or HTTP error responses.
 *
 * Usage:
 * {{{
 * import scalasql.simple.{*, given}
 * import SqliteDialect._
 *
 * given dbClient: DbClient = new DbClient.DataSource(dataSource, config = new {})
 *
 * @cask.database.transactional
 * @cask.get("/todos")
 * def list()(txn: Txn) = {
 *   txn.run(Todo.select)
 * }
 * }}}
 */
class transactional(using dbClient: DbClient) extends RawDecorator {

  def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] = {
    dbClient.transaction { txn =>
      val result = delegate(ctx, Map("txn" -> txn))

      val shouldRollback = result match {
        case _: cask.router.Result.Error => true
        case cask.router.Result.Success(response: Response[_]) if response.statusCode >= 400 => true
        case _ => false
      }

      if (shouldRollback) {
        txn.rollback()
      }

      result
    }
  }
}
