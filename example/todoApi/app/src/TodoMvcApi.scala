package app
object TodoMvcApi extends cask.MainRoutes{
  case class Todo(checked: Boolean, text: String)
  object Todo{
    implicit def todoRW = upickle.default.macroRW[Todo]
  }
  var todos = Seq(
    Todo(true, "Get started with Cask"),
    Todo(false, "Profit!")
  )

  @cask.get("/list/:state")
  def list(state: String) = {
    val filteredTodos = state match{
      case "all" => todos
      case "active" => todos.filter(!_.checked)
      case "completed" => todos.filter(_.checked)
    }
    upickle.default.write(filteredTodos)
  }

  @cask.post("/add")
  def add(request: cask.Request) = {
    todos = Seq(Todo(false, request.text())) ++ todos
  }

  @cask.post("/toggle/:index")
  def toggle(index: Int) = {
    todos = todos.updated(index, todos(index).copy(checked = !todos(index).checked))
  }

  @cask.post("/delete/:index")
  def delete(index: Int) = {
    todos = todos.patch(index, Nil, 1)
  }

  initialize()
}
