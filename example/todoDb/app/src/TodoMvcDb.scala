package app
import com.typesafe.config.ConfigFactory
import io.getquill.{SqliteJdbcContext, SnakeCase}
import io.getquill.context.ExecutionInfo

object TodoMvcDb extends cask.MainRoutes{
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")

  object ctx extends SqliteJdbcContext(
    SnakeCase,
    ConfigFactory.parseString(
      s"""{"driverClassName":"org.sqlite.JDBC","jdbcUrl":"jdbc:sqlite:$tmpDb/file.db"}"""
    )
  )

  class transactional extends cask.RawDecorator{
    class TransactionFailed(val value: cask.router.Result.Error) extends Exception
    def wrapFunction(pctx: cask.Request, delegate: Delegate) = {
      try ctx.transaction(
        delegate(Map()) match{
          case cask.router.Result.Success(t) => cask.router.Result.Success(t)
          case e: cask.router.Result.Error => throw new TransactionFailed(e)
        }
      )
      catch{case e: TransactionFailed => e.value}

    }
  }

  case class Todo(id: Int, checked: Boolean, text: String)
  object Todo{
    implicit def todoRW = upickle.default.macroRW[Todo]
  }

  ctx.executeAction(
    """CREATE TABLE todo (
      |  id INTEGER PRIMARY KEY AUTOINCREMENT,
      |  checked BOOLEAN,
      |  text TEXT
      |);
      |""".stripMargin
  )(ExecutionInfo.unknown, ())
  ctx.executeAction(
    """INSERT INTO todo (checked, text) VALUES
      |(1, 'Get started with Cask'),
      |(0, 'Profit!');
      |""".stripMargin
  )(ExecutionInfo.unknown, ())

  import ctx._

  @transactional
  @cask.get("/list/:state")
  def list(state: String) = {
    val filteredTodos = state match{
      case "all" => run(query[Todo])
      case "active" => run(query[Todo].filter(!_.checked))
      case "completed" => run(query[Todo].filter(_.checked))
    }
    upickle.default.write(filteredTodos)
  }

  @transactional
  @cask.post("/add")
  def add(request: cask.Request) = {
    val body = request.text()
    run(
      query[Todo]
        .insert(_.checked -> lift(false), _.text -> lift(body))
        .returningGenerated(_.id)
    )
  }

  @transactional
  @cask.post("/toggle/:index")
  def toggle(index: Int) = {
    run(query[Todo].filter(_.id == lift(index)).update(p => p.checked -> !p.checked))
  }

  @transactional
  @cask.post("/delete/:index")
  def delete(index: Int) = {
    run(query[Todo].filter(_.id == lift(index)).delete)
  }

  initialize()
}
