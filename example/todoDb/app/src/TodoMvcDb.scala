package app
import cask.database.transactional
import scalasql.DbApi.Txn
import scalasql.core.DbClient
import scalasql.simple.{*, given}
import SqliteDialect._

object TodoMvcDb extends cask.MainRoutes {
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")
  val sqliteDataSource = new org.sqlite.SQLiteDataSource()
  sqliteDataSource.setUrl(s"jdbc:sqlite:$tmpDb/file.db")

  given sqliteClient: DbClient = new DbClient.DataSource(
    sqliteDataSource,
    config = new {}
  )

  case class Todo(id: Int, checked: Boolean, text: String)

  object Todo extends SimpleTable[Todo] {
    given todoRW: upickle.default.ReadWriter[Todo] = upickle.default.macroRW[Todo]
  }

  sqliteClient.getAutoCommitClientConnection.updateRaw(
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

  @transactional
  @cask.get("/list/:state")
  def list(state: String)(txn: Txn) = {
    val filteredTodos = state match {
      case "all" => txn.run(Todo.select)
      case "active" => txn.run(Todo.select.filter(!_.checked))
      case "completed" => txn.run(Todo.select.filter(_.checked))
    }
    upickle.default.write(filteredTodos)
  }

  @transactional
  @cask.post("/add")
  def add(request: cask.Request)(txn: Txn) = {
    val body = request.text()
    txn.run(
      Todo
        .insert
        .columns(_.checked := false, _.text := body)
        .returning(_.id)
        .single
    )

    if (body == "FORCE FAILURE") throw new Exception("FORCE FAILURE BODY")
  }

  @transactional
  @cask.post("/toggle/:index")
  def toggle(index: Int)(txn: Txn) = {
    txn.run(Todo.update(_.id === index).set(p => p.checked := !p.checked))
  }

  @transactional
  @cask.post("/delete/:index")
  def delete(index: Int)(txn: Txn) = {
    txn.run(Todo.delete(_.id === index))
  }

  initialize()
}
