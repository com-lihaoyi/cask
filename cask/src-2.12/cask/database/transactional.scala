package cask.database

import cask.router.RawDecorator
import cask.model.Request

/**
 * Database support requires Scala 3.7+ for named tuples and SimpleTable support.
 * This is a stub for Scala 2.12 cross-compilation.
 */
class transactional extends RawDecorator {
  def wrapFunction(ctx: Request, delegate: Delegate) = {
    throw new UnsupportedOperationException(
      "@transactional decorator requires Scala 3.7+ with named tuples support"
    )
  }
}
