package cask

/**
 * Database support requires Scala 3.7+ for named tuples and SimpleTable support.
 *
 * This is a stub package for Scala 2.12 compatibility.
 */
package object database {
  // Stub types - will throw errors if used
  type DbClient = Nothing
  type Txn = Nothing
  type SimpleTable[T] = Nothing
}
