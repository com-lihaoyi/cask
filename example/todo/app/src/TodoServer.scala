package app
import com.typesafe.config.ConfigFactory
import io.getquill.{SnakeCase, SqliteJdbcContext}
import scalatags.Text.all._
import scalatags.Text.tags2

object TodoServer extends cask.MainRoutes{
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")

  object ctx extends SqliteJdbcContext(
    SnakeCase,
    ConfigFactory.parseString(
      s"""{"driverClassName":"org.sqlite.JDBC","jdbcUrl":"jdbc:sqlite:$tmpDb/file.db"}"""
    )
  )

  class transactional extends cask.RawDecorator{
    class TransactionFailed(val value: cask.router.Result.Error) extends Exception
    def wrapFunction(pctx: cask.Request, delegate: Delegate): OuterReturned = {
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

  import ctx._

  @transactional
  @cask.post("/list/:state")
  def list(state: String) = renderBody(state).render

  @transactional
  @cask.post("/add/:state")
  def add(state: String, request: cask.Request) = {
    val body = new String(request.readAllBytes())
    run(
      query[Todo]
        .insert(_.checked -> lift(false), _.text -> lift(body))
        .returningGenerated(_.id)
    )
    renderBody(state).render
  }

  @transactional
  @cask.post("/delete/:state/:index")
  def delete(state: String, index: Int) = {
    run(query[Todo].filter(_.id == lift(index)).delete)
    renderBody(state).render
  }

  @transactional
  @cask.post("/toggle/:state/:index")
  def toggle(state: String, index: Int) = {
    run(query[Todo].filter(_.id == lift(index)).update(p => p.checked -> !p.checked))
    renderBody(state).render
  }

  @transactional
  @cask.post("/clear-completed/:state")
  def clearCompleted(state: String) = {
    run(query[Todo].filter(_.checked).delete)
    renderBody(state).render
  }

  @transactional
  @cask.post("/toggle-all/:state")
  def toggleAll(state: String) = {
    val next = run(query[Todo].filter(_.checked).size) != 0
    run(query[Todo].update(_.checked -> !lift(next)))
    renderBody(state).render
  }

  def renderBody(state: String) = {
    val filteredTodos = state match{
      case "all" => run(query[Todo]).sortBy(-_.id)
      case "active" => run(query[Todo].filter(!_.checked)).sortBy(-_.id)
      case "completed" => run(query[Todo].filter(_.checked)).sortBy(-_.id)
    }
    frag(
      header(cls := "header",
        h1("todos"),
        input(cls := "new-todo", placeholder := "What needs to be done?", autofocus := "")
      ),
      tags2.section(cls := "main",
        input(
          id := "toggle-all",
          cls := "toggle-all",
          `type` := "checkbox",
          if (run(query[Todo].filter(_.checked).size != 0)) checked else ()
        ),
        label(`for` := "toggle-all","Mark all as complete"),
        ul(cls := "todo-list",
          for(todo <- filteredTodos) yield li(
            if (todo.checked) cls := "completed" else (),
            div(cls := "view",
              input(
                cls := "toggle",
                `type` := "checkbox",
                if (todo.checked) checked else (),
                data("todo-index") := todo.id
              ),
              label(todo.text),
              button(cls := "destroy", data("todo-index") := todo.id)
            ),
            input(cls := "edit", value := todo.text)
          )
        )
      ),
      footer(cls := "footer",
        span(cls := "todo-count",
          strong(run(query[Todo].filter(!_.checked).size).toInt),
          " items left"
        ),
        ul(cls := "filters",
          li(cls := "todo-all",
            a(if (state == "all") cls := "selected" else (), "All")
          ),
          li(cls := "todo-active",
            a(if (state == "active") cls := "selected" else (), "Active")
          ),
          li(cls := "todo-completed",
            a(if (state == "completed") cls := "selected" else (), "Completed")
          )
        ),
        button(cls := "clear-completed","Clear completed")
      )
    )
  }

  @transactional
  @cask.get("/")
  def index() = {
    cask.Response(
      "<!doctype html>" + html(lang := "en",
        head(
          meta(charset := "utf-8"),
          meta(name := "viewport", content := "width=device-width, initial-scale=1"),
          tags2.title("Template • TodoMVC"),
          link(rel := "stylesheet", href := "/static/index.css")
        ),
        body(
          tags2.section(cls := "todoapp", renderBody("all")),
          footer(cls := "info",
            p("Double-click to edit a todo"),
            p("Created by ",
              a(href := "http://todomvc.com","Li Haoyi")
            ),
            p("Part of ",
              a(href := "http://todomvc.com","TodoMVC")
            )
          ),
          script(src := "/static/app.js")
        )
      )
    )
  }

  @cask.staticResources("/static")
  def static() = "todo"

  initialize()
}
