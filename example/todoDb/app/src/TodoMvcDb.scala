package app
import scalasql.simple.{*, given}
import SqliteDialect._

object TodoMvcDb extends cask.MainRoutes {
  // Database path from config, fallback to temp directory
  val dbPath = cask.Config.getStringOpt("database.path") match {
    case Some("temp") | None =>
      java.nio.file.Files.createTempDirectory("todo-cask-sqlite").toString
    case Some(path) =>
      val dir = java.nio.file.Paths.get(path)
      java.nio.file.Files.createDirectories(dir)
      path
  }

  val sqliteDataSource = new org.sqlite.SQLiteDataSource()
  sqliteDataSource.setUrl(s"jdbc:sqlite:$dbPath/file.db")

  given dbClient: scalasql.core.DbClient = new DbClient.DataSource(
    sqliteDataSource,
    config = new {}
  )

  case class Todo(id: Int, checked: Boolean, text: String)

  object Todo extends SimpleTable[Todo] {
    given todoRW: upickle.default.ReadWriter[Todo] = upickle.default.macroRW[Todo]
  }

  // Initialize database schema
  dbClient.getAutoCommitClientConnection.updateRaw(
    """CREATE TABLE todo (
      |  id INTEGER PRIMARY KEY AUTOINCREMENT,
      |  checked BOOLEAN,
      |  text TEXT
      |);""".stripMargin
  )

  // Insert initial data if enabled in config
  if (cask.Config.getBooleanOpt("initial-data.enabled").getOrElse(true)) {
    dbClient.getAutoCommitClientConnection.updateRaw(
      """INSERT INTO todo (checked, text) VALUES
        |(1, 'Get started with Cask'),
        |(0, 'Profit!');""".stripMargin
    )
  }

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
