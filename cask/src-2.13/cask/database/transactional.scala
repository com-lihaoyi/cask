package cask.database

import cask.router.{RawDecorator, Result}
import cask.model.Request
import cask.model.Response.Raw

/**
 * Database support requires Scala 3.7+ for named tuples and SimpleTable support.
 * This is a stub for Scala 2.13 cross-compilation.
 */
class transactional extends RawDecorator {
  def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] = {
    throw new UnsupportedOperationException(
      "@transactional decorator requires Scala 3.7+ with named tuples support"
    )
  }
}
