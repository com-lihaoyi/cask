package todo
import scalatags.Text.all._
import scalatags.Text.tags2
import scalatags.Text.tags2.section
case class Todo(checked: Boolean, text: String)
object Server extends cask.MainRoutes{
  var todos = Seq(
    Todo(true, "Get started with Cask"),
    Todo(false, "Profit!")
  )

  @cask.get("/list/:state")
  def list(state: String) = list0(state).render

  @cask.post("/add/:state")
  def add(state: String, request: cask.Request) = {
    todos = Seq(Todo(false, new String(request.data.readAllBytes()))) ++ todos
    list0(state).render
  }

  def list0(state: String) = {
    val filteredTodos = state match{
      case "all" => todos
      case "active" => todos.filter(!_.checked)
      case "completed" => todos.filter(_.checked)
    }
    frag(
      for((todo, i) <- filteredTodos.zipWithIndex) yield li(if (todo.checked) cls := "completed" else (),
        div(cls := "view",
          input(cls := "toggle", `type` := "checkbox", if (todo.checked) checked else ()),
          label(todo.text),
          button(cls := "destroy", data("todo-index") := i)
        ),
        input(cls := "edit", value := todo.text)
      )
    )
  }

  @cask.get("/")
  def index() = {
    cask.Response(
      "<!doctype html>" + html(lang := "en",
        head(
          meta(charset := "utf-8"),
          meta(name := "viewport", content := "width=device-width, initial-scale=1"),
          tags2.title("Template • TodoMVC"),
          link(rel := "stylesheet", href := "/static/base.css"),
          link(rel := "stylesheet", href := "/static/index.css")
        ),
        body(
          section(cls := "todoapp",
            header(cls := "header",
              h1("todos"),
              input(cls := "new-todo", placeholder := "What needs to be done?", autofocus := "")
            ),
            section(cls := "main",
              input(id := "toggle-all", cls := "toggle-all", `type` := "checkbox"),
              label(`for` := "toggle-all","Mark all as complete"),
              ul(cls := "todo-list",
                list0("all")
              )
            ),
            footer(cls := "footer",
              span(cls := "todo-count",
                strong("0"),
                "item left"
              ),
              ul(cls := "filters",
                li(
                  a(cls := "selected", href := "#/","All")
                ),
                li(
                  a(href := "#/active","Active")
                ),
                li(
                  a(href := "#/completed","Completed")
                )
              ),
              button(cls := "clear-completed","Clear completed")
            )
          ),
          footer(cls := "info",
            p("Double-click to edit a todo"),
            p("Template by",
              a(href := "http://sindresorhus.com","Sindre Sorhus")
            ),
            p("Created by",
              a(href := "http://todomvc.com","you")
            ),
            p("Part of",
              a(href := "http://todomvc.com","TodoMVC")
            )
          ),
          script(src := "node_modules/todomvc-common/base.js"),
          script(src := "/static/app.js")
        )
      )
    )
  }

  @cask.static("/static")
  def static() = "example/todo/resources/todo"

  initialize()
}
