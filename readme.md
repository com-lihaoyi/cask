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
import cask.model.ParamContext

object Decorated extends cask.MainRoutes{
  class User{
    override def toString = "[haoyi]"
  }
  class loggedIn extends cask.Decorator {
    def getRawParams(ctx: ParamContext) = Right(cask.Decor("user" -> new User()))
  }
  class withExtra extends cask.Decorator {
    def getRawParams(ctx: ParamContext) = Right(cask.Decor("extra" -> 31337))
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

Writing Custom Endpoints
------------------------

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

This is a simple self-contained example of using Cask to write an API server for
the common [TodoMVC example app](http://todomvc.com/).

This minimal example intentionally does not contain javascript, HTML, styles,
etc.. Those can be managed via the normal mechanism for
[Serving Static Files](#serving-static-files).
