package test.cask
import cask.internal.Router
import cask.model.{ParamContext, Response}
import com.typesafe.config.ConfigFactory
import io.getquill._


object TodoMvcDb extends cask.MainRoutes{
  case class Todo(id: Int, checked: Boolean, text: String)
  object Todo{
    implicit def todoRW = upickle.default.macroRW[Todo]
  }
  object ctx extends SqliteJdbcContext(
    SnakeCase,
    ConfigFactory.parseString(
      s"""{"driverClassName":"org.sqlite.JDBC","jdbcUrl":"jdbc:sqlite:$tmpDb/file.db"}"""
    )
  )
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")

  import ctx._

  class transactional extends cask.Decorator{
    def wrapMethodOutput(pctx: ParamContext,
                         delegate: Map[String, Input] => Router.Result[Response]):  Router.Result[Response] = {
      ctx.transaction(delegate(Map("ctx" -> ctx)))
    }
  }

  ctx.executeAction(
    """CREATE TABLE todo (
      |  id INTEGER PRIMARY KEY AUTOINCREMENT,
      |  checked BOOLEAN,
      |  text TEXT
      |);
      |""".stripMargin
  )
  ctx.executeAction(
    """INSERT INTO todo (checked, text) VALUES
      |(1, 'Get started with Cask'),
      |(0, 'Profit!');
      |""".stripMargin
  )

  @transactional
  @cask.get("/list/:state")
  def list(state: String)(ctx: SqliteJdbcContext[_]) = {
    val filteredTodos = state match{
      case "all" => run(query[Todo])
      case "active" => run(query[Todo].filter(!_.checked))
      case "completed" => run(query[Todo].filter(_.checked))
    }
    upickle.default.write(filteredTodos)
  }

  @transactional
  @cask.post("/add")
  def add(request: cask.Request)(ctx: SqliteJdbcContext[_]) = {
    val body = new String(request.data.readAllBytes())
    run(query[Todo].insert(_.checked -> lift(false), _.text -> lift(body)).returning(_.id))
  }

  @transactional
  @cask.post("/toggle/:index")
  def toggle(index: Int)(ctx: SqliteJdbcContext[_]) = {
    run(query[Todo].filter(_.id == lift(index)).update(p => p.checked -> !p.checked))
  }

  @transactional
  @cask.post("/delete/:index")
  def delete(index: Int)(ctx: SqliteJdbcContext[_]) = {
    run(query[Todo].filter(_.id == lift(index)).delete)

  }

  initialize()
}
