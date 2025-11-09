package cask.database

import cask.router.RawDecorator
import cask.model.{Request, Response}

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
 * given dbClient: scalasql.core.DbClient = new DbClient.DataSource(dataSource, config = new {})
 *
 * @cask.database.transactional[scalasql.core.DbClient]
 * @cask.get("/todos")
 * def list()(using ctx: scalasql.core.DbApi.Txn) = {
 *   ctx.run(Todo.select)
 * }
 * }}}
 */
class transactional[T <: AnyRef](using dbClient: T) extends RawDecorator {

  def wrapFunction(ctx: Request, delegate: Delegate) = {
    // Use reflection to call methods on dbClient without importing scalasql
    // This works for both Scala 3.3.4 and 3.7.3
    val client = dbClient.asInstanceOf[Any]
    val transactionMethod = client.getClass.getMethod("transaction", classOf[Function1[Any, Any]])

    transactionMethod.invoke(client, new Function1[Any, Any] {
      def apply(txn: Any): Any = {
        val result = delegate(ctx, Map("ctx" -> txn))

        val shouldRollback = result match {
          case _: cask.router.Result.Error => true
          case cask.router.Result.Success(response: Response[_]) if response.statusCode >= 400 => true
          case _ => false
        }

        if (shouldRollback) {
          // Use reflection to call rollback
          val rollbackMethod = txn.getClass.getMethod("rollback")
          rollbackMethod.invoke(txn)
        }

        result
      }
    }).asInstanceOf[cask.router.Result[Response.Raw]]
  }
}
