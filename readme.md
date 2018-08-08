Cask: a Scala HTTP micro-framework
==================================

```scala
object MinimalApplication extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    new String(request.data.readAllBytes()).reverse
  }

  initialize()
}
```

Cask is a simple Scala web framework inspired by Python's
[Flask](http://flask.pocoo.org/docs/1.0/) project. It aims to bring simplicity,
flexibility and ease-of-use to Scala webservers, avoiding cryptic DSLs or
complicated asynchrony.

Getting Started
---------------

The easiest way to begin using Cask is by downloading the
[Mill](http://www.lihaoyi.com/mill/) example project:

- Install [Mill](http://www.lihaoyi.com/mill/)
- Unzip [XXX](XXX) into a folder. This should give you the following files:
```text
build.sc
app/src/MinimalExample.scala
app/test/src/ExampleTests.scala
```

- `cd` into the folder, and run

```bash
mill -w app.runBackground
```

This will server up the Cask application on `http://localhost:8080`. You can
immediately start interacting with it either via the browser, or
programmatically via `curl` or a HTTP client like
[Requests-Scala](https://github.com/lihaoyi/requests-scala):

```scala
val host = "http://localhost:8080"

val success = requests.get(host)

success.text() ==> "Hello World!"
success.statusCode ==> 200

requests.get(host + "/doesnt-exist").statusCode ==> 404

requests.post(host + "/do-thing", data = "hello").text() ==> "olleh"

requests.get(host + "/do-thing").statusCode ==> 404
```

These HTTP calls are part of the test suite for the example project, which you
can run using:

```bash
mill -w app.test
```

Cask is just a Scala library, and you can use Cask in any existing Scala project
via the following coordinates:

```scala
// Mill
ivy"com.lihaoyi::cask:0.1.0"

// SBT
"com.lihaoyi" %% "cask" % "0.1.0"
```

Minimal Example
---------------
```scala
object MinimalApplication extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    new String(request.data.readAllBytes()).reverse
  }

  initialize()
}
```

The rough outline of how the minimal example works should be easy to understand:

- You define an object that inherits from `cask.MainRoutes`

- Define endpoints using annotated functions, using `@cask.get` or `@cask.post`
  with the route they should match

- Each function can return the data you want in the response, or a
  `cask.Response` if you want further customization: response code, headers,
  etc.

- Your function can tale an optional `cask.Request`, which exposes the entire
  incoming HTTP request if necessary. In the above example, we use it to read
  the request body into a string and return it reversed.

In most cases, Cask provides convenient helpers to extract exactly the data from
the incoming HTTP request that you need, while also de-serializing it into the
data type you need and returning meaningful errors if they are missing. Thus,
although you can always get all the data necessary through `cask.Request`, it is
often more convenient to use another way, which will go into below.

As your application grows, you will likely want to split up the routes into
separate files, themselves separate from any configuration of the Main
entrypoint (e.g. overriding the port, host, default error handlers, etc.). You
can do this by splitting it up into `cask.Routes` and `cask.Main` objects:

```scala
object MinimalRoutes extends cask.Routes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    new String(request.data.readAllBytes()).reverse
  }

  initialize()
}

object MinimalMain extends cask.Main(MinimalRoutes)
```

You can split up your routes into separate `cask.Routes` objects as makes sense
and pass them all into `cask.Main`.

Variable Routes
---------------

```scala
object VariableRoutes extends cask.MainRoutes{
  @cask.get("/user/:userName")
  def showUserProfile(userName: String) = {
    s"User $userName"
  }

  @cask.get("/post/:postId")
  def showPost(postId: Int, param: Seq[String]) = {
    s"Post $postId $param"
  }

  @cask.get("/path", subpath = true)
  def showSubpath(subPath: cask.Subpath) = {
    s"Subpath ${subPath.value}"
  }

  initialize()
}
```

You can bind variables to endpoints by declaring them as parameters: these are
either taken from a path-segment matcher of the same name (e.g. `postId` above),
or from query-parameters of the same name (e.g. `param` above). You can make
`param` take a `: String` to match `?param=hello`, an `: Int` for `?param=123`
or a `Seq[String]` (as above) for repeated params such as
`?param=hello&param=world`.

If you need to capture the entire sub-path of the request, you can set the flag
`subpath=true` and ask for a `: cask.Subpath` (the name of the param doesn't
matter). This will make the route match any sub-path of the prefix given to the
`@cask` decorator, and give you the remainder to use in your endpoint logic.

Receiving Form-encoded or JSON data
-----------------------------------

```scala
object FormJsonPost extends cask.MainRoutes{
  @cask.postJson("/json")
  def jsonEndpoint(value1: ujson.Js.Value, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postForm("/form")
  def formEndpoint(value1: cask.FormValue, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postForm("/upload")
  def uploadFile(image: cask.FormFile) = {
    image.fileName
  }

  initialize()
}
```

If you need to handle a JSON-encoded POST request, you can use the
`@cast.postJson` decorator. This assumes the posted request body is a JSON dict,
and uses its keys to populate the endpoint's parameters, either as raw
`ujson.Js.Value`s or deserialized into `Seq[Int]`s or other things.
Deserialization is handled using the
[uPickle](https://github.com/lihaoyi/upickle) JSON library, though you could
write your own version of `postJson` to work with any other JSON library of your
choice.

Similarly, you can mark endpoints as `@cask.postForm`, in which case the
endpoints params will be taken from the form-encoded POST body either raw (as
`cask.FormValue`s) or deserialized into simple data structures. Use
`cask.FormFile` if you want the given form value to be a file upload.

Both normal forms and multipart forms are handled the same way.

If the necessary keys are not present in the JSON/form-encoded POST body, or the
deserialization into Scala data-types fails, a 400 response is returned
automatically with a helpful error message.


Processing Cookies
------------------

```scala
object Cookies extends cask.MainRoutes{
  @cask.get("/read-cookie")
  def readCookies(username: cask.Cookie) = {
    username.value
  }

  @cask.get("/store-cookie")
  def storeCookies() = {
    cask.Response(
      "Cookies Set!",
      cookies = Seq(cask.Cookie("username", "the username"))
    )
  }

  @cask.get("/delete-cookie")
  def deleteCookie() = {
    cask.Response(
      "Cookies Deleted!",
      cookies = Seq(cask.Cookie("username", "", expires = java.time.Instant.EPOCH))
    )
  }

  initialize()
}
```

Cookies are most easily read by declaring a `: cask.Cookie` parameter; the
parameter name is used to fetch the cookie you are interested in. Cookies can be
stored by setting the `cookie` attribute in the response, and deleted simply by
setting `expires = java.time.Instant.EPOCH` (i.e. to have expired a long time
ago)

Serving Static Files
--------------------
```scala
object StaticFiles extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    "Hello!"
  }

  @cask.static("/static")
  def staticRoutes() = "cask/resources/cask"

  initialize()
}
```

You can ask Cask to serve static files by defining a `@cask.static` endpoint.
This will match any subpath of the value returned by the endpoint (e.g. above
`/static/file.txt`, `/static/folder/file.txt`, etc.) and return the file
contents from the corresponding file on disk (and 404 otherwise).

Redirects or Aborts
-------------------
```scala
object RedirectAbort extends cask.MainRoutes{
  @cask.get("/")
  def index() = {
    cask.Redirect("/login")
  }

  @cask.get("/login")
  def login() = {
    cask.Abort(401)
  }

  initialize()
}
```

Cask provides some convenient helpers `cask.Redirect` and `cask.Abort` which you
can return; these are simple wrappers around `cask.Request`, and simply set up
the relevant headers or status code for you.

Extending Endpoints with Decorators
-----------------------------------

```scala
object Decorated extends cask.MainRoutes{
  class User{
    override def toString = "[haoyi]"
  }
  class loggedIn extends cask.Decorator {
    def wrapFunction(ctx: cask.ParamContext, delegate: Delegate): Returned = {
      delegate(Map("user" -> new User()))
    }
  }
  class withExtra extends cask.Decorator {
    def wrapFunction(ctx: cask.ParamContext, delegate: Delegate): Returned = {
      delegate(Map("extra" -> 31337))
    }
  }

  @withExtra()
  @cask.get("/hello/:world")
  def hello(world: String)(extra: Int) = {
    world + extra
  }

  @loggedIn()
  @cask.get("/internal/:world")
  def internal(world: String)(user: User) = {
    world + user
  }

  @withExtra()
  @loggedIn()
  @cask.get("/internal-extra/:world")
  def internalExtra(world: String)(user: User)(extra: Int) = {
    world + user + extra
  }

  @withExtra()
  @loggedIn()
  @cask.get("/ignore-extra/:world")
  def ignoreExtra(world: String)(user: User) = {
    world + user
  }

  initialize()
}
```

You can write extra decorator annotations that stack on top of the existing
`@cask.get`/`@cask.post` to provide additional arguments or validation. This is
done by implementing the `cask.Decorator` interface and it's `getRawParams`
function. `getRawParams`:

- Receives a `ParamContext`, which basically gives you full access to the
  underlying undertow HTTP connection so you can pick out whatever data you
  would like

- Returns an `Either[Response, cask.Decor[Any]]`. Returning a `Left` lets you
  bail out early with a fixed `cask.Response`, avoiding further processing.
  Returning a `Right` provides a map of parameter names and values that will
  then get passed to the endpoint function in consecutive parameter lists (shown
  above), as well as an optional cleanup function that is run after the endpoint
  terminates.

Each additional decorator is responsible for one additional parameter list to
the right of the existing parameter lists, each of which can contain any number
of parameters.

Decorators are useful for things like:

- Making an endpoint return a HTTP 403 if the user isn't logged in, but if they are
  logged in providing the `: User` object to the body of the endpoint function

- Rate-limiting users by returning early with a HTTP 429 if a user tries to
  access an endpoint too many times too quickly

- Providing request-scoped values to the endpoint function: perhaps a database
  transaction that commits when the function succeeds (and rolls-back if it
  fails), or access to some system resource that needs to be released.

TodoMVC Api Server
------------------

```scala
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
    todos = Seq(Todo(false, new String(request.data.readAllBytes()))) ++ todos
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
```

This is a simple self-contained example of using Cask to write an in-memory API
server for the common [TodoMVC example app](http://todomvc.com/).

This minimal example intentionally does not contain javascript, HTML, styles,
etc.. Those can be managed via the normal mechanism for
[Serving Static Files](#serving-static-files).


TodoMVC Database Integration
----------------------------
```scala
import cask.internal.Router
import com.typesafe.config.ConfigFactory
import io.getquill.{SqliteJdbcContext, SnakeCase}

object TodoMvcDb extends cask.MainRoutes{
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")

  object ctx extends SqliteJdbcContext(
    SnakeCase,
    ConfigFactory.parseString(
      s"""{"driverClassName":"org.sqlite.JDBC","jdbcUrl":"jdbc:sqlite:$tmpDb/file.db"}"""
    )
  )

  class transactional extends cask.Decorator{
    class TransactionFailed(val value: Router.Result.Error) extends Exception
    def wrapFunction(pctx: cask.ParamContext, delegate: Delegate): Returned = {
      try ctx.transaction(
        delegate(Map()) match{
          case Router.Result.Success(t) => Router.Result.Success(t)
          case e: Router.Result.Error => throw new TransactionFailed(e)
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
  )
  ctx.executeAction(
    """INSERT INTO todo (checked, text) VALUES
      |(1, 'Get started with Cask'),
      |(0, 'Profit!');
      |""".stripMargin
  )

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
    val body = new String(request.data.readAllBytes())
    run(query[Todo].insert(_.checked -> lift(false), _.text -> lift(body)).returning(_.id))
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

```

This example demonstrates how to use Cask to write a TodoMVC API server that
persists it's state in a database rather than in memory. We use the
[Quill](http://getquill.io/) database access library to write a `@transactional`
decorator that automatically opens one transaction per call to an endpoint,
ensuring that database queries are properly committed on success or rolled-back
on error. Note that because the default database connector propagates its
transaction context in a thread-local, `@transactional` does not need to pass
the `ctx` object into each endpoint as an additional parameter list, and so we
simply leave it out.

While this example is specific to Quill, you can easily modify the
`@transactional` decorator to make it work with whatever database access library
you happen to be using. For libraries which need an implicit transaction, it can
be passed into each endpoint function as an additional parameter list as
described in
[Extending Endpoints with Decorators](#extending-endpoints-with-decorators).

TodoMVC Full Stack Web
----------------------

The following code snippet is the complete code for a full-stack TodoMVC
implementation: including HTML generation for the web UI via
[Scalatags](https://github.com/lihaoyi/scalatags), Javascript for the
interactivity, static file serving, and database integration via
[Quill](https://github.com/getquill/quill). While slightly long, this example
should give you a tour of all the things you need to know to use Cask.

Note that this is a "boring" server-side-rendered webapp with Ajax interactions,
without any complex front-end frameworks or libraries: it's purpose is to
demonstrate a simple working web application of using Cask end-to-end, which you
can build upon to create your own Cask web application architected however you
would like.

```scala
import cask.internal.Router
import com.typesafe.config.ConfigFactory
import io.getquill.{SnakeCase, SqliteJdbcContext}
import scalatags.Text.all._
import scalatags.Text.tags2
object Server extends cask.MainRoutes{
  val tmpDb = java.nio.file.Files.createTempDirectory("todo-cask-sqlite")

  object ctx extends SqliteJdbcContext(
    SnakeCase,
    ConfigFactory.parseString(
      s"""{"driverClassName":"org.sqlite.JDBC","jdbcUrl":"jdbc:sqlite:$tmpDb/file.db"}"""
    )
  )

  class transactional extends cask.Decorator{
    class TransactionFailed(val value: Router.Result.Error) extends Exception
    def wrapFunction(pctx: cask.ParamContext, delegate: Delegate): Returned = {
      try ctx.transaction(
        delegate(Map()) match{
          case Router.Result.Success(t) => Router.Result.Success(t)
          case e: Router.Result.Error => throw new TransactionFailed(e)
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
    val body = new String(request.data.readAllBytes())
    run(query[Todo].insert(_.checked -> lift(false), _.text -> lift(body)).returning(_.id))
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

  @cask.static("/static")
  def static() = "example/todo/resources/todo"

  initialize()
}
```
