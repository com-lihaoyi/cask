package app
import scalasql.namedtuples.SimpleTable
import scalasql.SqliteDialect._

import java.util.concurrent.{ExecutorService, Executors}

object TodoMvcDbWithLoom extends cask.MainRoutes {
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")
  val sqliteDataSource = new org.sqlite.SQLiteDataSource()
  sqliteDataSource.setUrl(s"jdbc:sqlite:$tmpDb/file.db")

  given dbClient: scalasql.DbClient = new scalasql.DbClient.DataSource(
    sqliteDataSource,
    config = new scalasql.Config {}
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

  @cask.transactional
  @cask.get("/list/:state")
  def list(state: String)(using ctx: scalasql.DbClient.Txn) = {
    val filteredTodos = state match {
      case "all" => ctx.run(Todo.select)
      case "active" => ctx.run(Todo.select.filter(!_.checked))
      case "completed" => ctx.run(Todo.select.filter(_.checked))
    }
    upickle.default.write(filteredTodos)
  }

  @cask.transactional
  @cask.post("/add")
  def add(request: cask.Request)(using ctx: scalasql.DbClient.Txn) = {
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

  @cask.transactional
  @cask.post("/toggle/:index")
  def toggle(index: Int)(using ctx: scalasql.DbClient.Txn) = {
    ctx.run(Todo.update(_.id === index).set(p => p.checked := !p.checked))
  }

  @cask.transactional
  @cask.post("/delete/:index")
  def delete(index: Int)(using ctx: scalasql.DbClient.Txn) = {
    ctx.run(Todo.delete(_.id === index))
  }

  initialize()
}
