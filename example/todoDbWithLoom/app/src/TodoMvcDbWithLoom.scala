package app
import scalasql.simple.{*, given}
import SqliteDialect._

import java.util.concurrent.{ExecutorService, Executors}

object TodoMvcDbWithLoom extends cask.MainRoutes {
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")
  val sqliteDataSource = new org.sqlite.SQLiteDataSource()
  sqliteDataSource.setUrl(s"jdbc:sqlite:$tmpDb/file.db")

  given dbClient: scalasql.core.DbClient = new DbClient.DataSource(
    sqliteDataSource,
    config = new {}
  )

  private val executor = Executors.newFixedThreadPool(4)
  override protected def handlerExecutor(): Option[ExecutorService] = {
    super.handlerExecutor().orElse(Some(executor))
  }

  case class Todo(id: Int, checked: Boolean, text: String)

  object Todo extends SimpleTable[Todo] {
    given todoRW: upickle.default.ReadWriter[Todo] = upickle.default.macroRW[Todo]
  }

  dbClient.getAutoCommitClientConnection.updateRaw(
    """CREATE TABLE todo (
      |  id INTEGER PRIMARY KEY AUTOINCREMENT,
      |  checked BOOLEAN,
      |  text TEXT
      |);
      |
      |INSERT INTO todo (checked, text) VALUES
      |(1, 'Get started with Cask'),
      |(0, 'Profit!');
      |""".stripMargin
  )

  @cask.database.transactional[scalasql.core.DbClient]
  @cask.get("/list/:state")
  def list(state: String)(using ctx: scalasql.core.DbApi.Txn) = {
    val filteredTodos = state match {
      case "all" => ctx.run(Todo.select)
      case "active" => ctx.run(Todo.select.filter(!_.checked))
      case "completed" => ctx.run(Todo.select.filter(_.checked))
    }
    upickle.default.write(filteredTodos)
  }

  @cask.database.transactional[scalasql.core.DbClient]
  @cask.post("/add")
  def add(request: cask.Request)(using ctx: scalasql.core.DbApi.Txn) = {
    val body = request.text()
    ctx.run(
      Todo
        .insert
        .columns(_.checked := false, _.text := body)
        .returning(_.id)
        .single
    )

    if (body == "FORCE FAILURE") throw new Exception("FORCE FAILURE BODY")
  }

  @cask.database.transactional[scalasql.core.DbClient]
  @cask.post("/toggle/:index")
  def toggle(index: Int)(using ctx: scalasql.core.DbApi.Txn) = {
    ctx.run(Todo.update(_.id === index).set(p => p.checked := !p.checked))
  }

  @cask.database.transactional[scalasql.core.DbClient]
  @cask.post("/delete/:index")
  def delete(index: Int)(using ctx: scalasql.core.DbApi.Txn) = {
    ctx.run(Todo.delete(_.id === index))
  }

  initialize()
}
