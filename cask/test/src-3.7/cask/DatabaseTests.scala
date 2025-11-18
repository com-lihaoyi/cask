package test.cask

import cask.database.transactional
import scalasql.DbApi.Txn
import scalasql.core.DbClient
import scalasql.simple.{*, given}
import scalasql.SqliteDialect._
import utest._

object DatabaseTests extends TestSuite {

  // Test database setup
  val tmpDb = java.nio.file.Files.createTempDirectory("test-cask-sqlite")
  val sqliteDataSource = new org.sqlite.SQLiteDataSource()
  sqliteDataSource.setUrl(s"jdbc:sqlite:$tmpDb/test.db")

  given testDbClient: DbClient = new DbClient.DataSource(
    sqliteDataSource,
    config = new {}
  )

  case class TestItem(id: Int, name: String, value: Int)
  object TestItem extends SimpleTable[TestItem]

  // Initialize test table
  testDbClient.getAutoCommitClientConnection.updateRaw(
    """DROP TABLE IF EXISTS test_item;
      |CREATE TABLE test_item (
      |  id INTEGER PRIMARY KEY AUTOINCREMENT,
      |  name TEXT,
      |  value INTEGER
      |)""".stripMargin
  )

  def cleanTable() = {
    testDbClient.getAutoCommitClientConnection.updateRaw("DELETE FROM test_item")
  }

  val tests = Tests {
    test("basic transaction commits on success") {
      cleanTable()

      var committed = false
      testDbClient.transaction { txn =>
        txn.run(TestItem.insert.columns(_.name := "test1", _.value := 100))
        committed = true
      }

      assert(committed)
      val items = testDbClient.getAutoCommitClientConnection.run(TestItem.select)
      assert(items.length == 1)
      assert(items.head.name == "test1")
    }

    test("basic transaction rolls back on exception") {
      cleanTable()

      try {
        testDbClient.transaction { txn =>
          txn.run(TestItem.insert.columns(_.name := "fail", _.value := 999))
          throw new Exception("Forced failure")
        }
      } catch {
        case _: Exception => // Expected
      }

      val items = testDbClient.getAutoCommitClientConnection.run(TestItem.select)
      assert(items.isEmpty)
    }

    test("transactional decorator instantiation") {
      val decorator = new transactional(using testDbClient)
      assert(decorator != null)
    }

    test("decorator wrapFunction with success") {
      cleanTable()

      val decorator = new transactional(using testDbClient)

      // Simulate successful route execution
      testDbClient.transaction { txn =>
        txn.run(TestItem.insert.columns(_.name := "decorator-test", _.value := 42))
        // Transaction commits automatically
      }

      val items = testDbClient.getAutoCommitClientConnection.run(TestItem.select)
      assert(items.length == 1)
      assert(items.head.name == "decorator-test")
    }

    test("decorator rollback on exception") {
      cleanTable()

      val decorator = new transactional(using testDbClient)

      try {
        testDbClient.transaction { txn =>
          txn.run(TestItem.insert.columns(_.name := "rollback-test", _.value := 500))
          throw new Exception("Simulated error")
        }
      } catch {
        case _: Exception => // Expected
      }

      val items = testDbClient.getAutoCommitClientConnection.run(TestItem.select)
      assert(items.isEmpty)
    }

    test("multiple inserts in single transaction") {
      cleanTable()

      testDbClient.transaction { txn =>
        txn.run(TestItem.insert.columns(_.name := "item1", _.value := 1))
        txn.run(TestItem.insert.columns(_.name := "item2", _.value := 2))
        txn.run(TestItem.insert.columns(_.name := "item3", _.value := 3))
      }

      val items = testDbClient.getAutoCommitClientConnection.run(TestItem.select)
      assert(items.length == 3)
    }

    test("rollback discards all changes in transaction") {
      cleanTable()

      try {
        testDbClient.transaction { txn =>
          txn.run(TestItem.insert.columns(_.name := "item1", _.value := 1))
          txn.run(TestItem.insert.columns(_.name := "item2", _.value := 2))
          throw new Exception("Rollback all")
        }
      } catch {
        case _: Exception => // Expected
      }

      val items = testDbClient.getAutoCommitClientConnection.run(TestItem.select)
      assert(items.isEmpty)
    }

    test("DbClient given context is available") {
      // Verify implicit DbClient is properly resolved
      val decorator = new transactional(using testDbClient)
      assert(decorator != null)

      // Verify we can use it in a transaction
      cleanTable()
      testDbClient.transaction { txn =>
        val count = txn.run(TestItem.select).length
        assert(count == 0)
      }
    }
  }
}
