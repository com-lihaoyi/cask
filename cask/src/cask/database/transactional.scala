package cask.database

import scalasql.DbClient
import cask.router.RawDecorator
import cask.model.{Request, Response}

/**
 * Decorator that wraps route execution in a database transaction.
 *
 * Automatically commits on success and rolls back on exceptions or HTTP error responses.
 */
class transactional(using dbClient: DbClient) extends RawDecorator {

  def wrapFunction(ctx: Request, delegate: Delegate) = {
    dbClient.transaction { txn =>
      val result = delegate(ctx, Map("ctx" -> txn))

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
