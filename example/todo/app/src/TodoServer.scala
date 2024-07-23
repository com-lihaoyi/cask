package app
import scalasql.DbApi.Txn
import scalasql.Sc
import scalasql.SqliteDialect._
import scalatags.Text.all._
import scalatags.Text.tags2

object TodoServer extends cask.MainRoutes{
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")

  val sqliteDataSource = new org.sqlite.SQLiteDataSource()
  sqliteDataSource.setUrl(s"jdbc:sqlite:$tmpDb/file.db")
  lazy val sqliteClient = new scalasql.DbClient.DataSource(
    sqliteDataSource,
    config = new scalasql.Config {}
  )

  class transactional extends cask.RawDecorator{
    def wrapFunction(pctx: cask.Request, delegate: Delegate) = {
      sqliteClient.transaction { txn =>
        val res = delegate(ctx, Map("txn" -> txn))
        if (res.isInstanceOf[cask.router.Result.Error]) txn.rollback()
        res
      }
    }
  }

  case class Todo[T[_]](id: T[Int], checked: T[Boolean], text: T[String])
  object Todo extends scalasql.Table[Todo]

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
  @cask.post("/list/:state")
  def list(state: String)(txn: Txn) = renderBody(state)(txn).render

  @transactional
  @cask.post("/add/:state")
  def add(state: String, request: cask.Request)(implicit txn: Txn) = {
    val body = request.text()
    txn.run(Todo.insert.columns(_.checked := false, _.text := body))
    renderBody(state).render
  }

  @transactional
  @cask.post("/delete/:state/:index")
  def delete(state: String, index: Int)(implicit txn: Txn) = {
    txn.run(Todo.delete(_.id === index))
    renderBody(state).render
  }

  @transactional
  @cask.post("/toggle/:state/:index")
  def toggle(state: String, index: Int)(implicit txn: Txn) = {
    txn.run(Todo.update(_.id === index).set(p => p.checked := !p.checked))
    renderBody(state).render
  }

  @transactional
  @cask.post("/clear-completed/:state")
  def clearCompleted(state: String)(implicit txn: Txn) = {
    txn.run(Todo.delete(_.checked))
    renderBody(state).render
  }

  @transactional
  @cask.post("/toggle-all/:state")
  def toggleAll(state: String)(implicit txn: Txn) = {
    val next = txn.run(Todo.select.filter(_.checked).size) != 0
    txn.run(Todo.update(_ => true).set(_.checked := !next))
    renderBody(state).render
  }

  def renderBody(state: String)(implicit txn: Txn) = {
    val filteredTodos = state match{
      case "all" => txn.run(Todo.select).sortBy(-_.id)
      case "active" => txn.run(Todo.select.filter(!_.checked)).sortBy(-_.id)
      case "completed" => txn.run(Todo.select.filter(_.checked)).sortBy(-_.id)
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
          if (txn.run(Todo.select.filter(_.checked).size !== 0)) checked else ()
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
          strong(txn.run(Todo.select.filter(!_.checked).size).toInt),
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
  def index()(implicit txn: Txn) = {
    doctype("html")(
      html(lang := "en",
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
