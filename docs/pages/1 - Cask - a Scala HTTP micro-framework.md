

[![Discord Chat][discord-badge]][discord-link]
[![Patreon][patreon-badge]][patreon-link]


[discord-badge]: https://img.shields.io/badge/discord-join_chat-61AA8F.svg?logo=discord
[discord-link]: https://discord.gg/scala
[patreon-badge]: https://img.shields.io/badge/patreon-sponsor-ff69b4.svg
[patreon-link]: https://www.patreon.com/lihaoyi


$$$minimalApplication

[Cask](https://github.com/lihaoyi/cask) is a simple Scala web framework inspired
by Python's [Flask](http://flask.pocoo.org/docs/1.0/) project. It aims to bring
simplicity, flexibility and ease-of-use to Scala webservers, avoiding cryptic
DSLs or complicated asynchrony.

Getting Started
---------------

The easiest way to begin using Cask is by downloading the example project above.

Unzip one of the example projects available on this page (e.g. above) into a
folder. This should give you the following files:

```text
build.sc
app/src/MinimalExample.scala
app/test/src/ExampleTests.scala
```

- `cd` into the folder, and run

```bash
./mill -w app.runBackground
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
./mill -w app.test
```

To configure your Cask application to work with IntelliJ, you can use:

```bash
./mill mill.scalalib.GenIdea/idea
```

This will need to be re-run when you re-configure your `build.sc` file, e.g.
when adding additional modules or third-party dependencies.

Cask is just a Scala library, and you can use Cask in any existing Scala project
via the following coordinates:

```scala
// Mill
ivy"com.lihaoyi::cask:0.9.5"

// SBT
"com.lihaoyi" %% "cask" % "0.9.5"
```

The `./mill` command is just a wrapper around the
[Mill build tool](http://www.lihaoyi.com/mill/); the `build.sc` files you see in
all examples are Mill build files, and you can use your own installation of Mill
instead of `./mill` if you wish. All normal Mill commands and functionality
works for `./mill`.

The following examples will walk you through how to use Cask to accomplish tasks
common to anyone writing a web application. Each example comes with a
downloadable example project with code and unit tests, which you can use via the
same `./mill -w app.runBackground` or `./mill -w app.test` workflows we saw above.

## Minimal Example

$$$minimalApplication

The rough outline of how the minimal example works should be easy to understand:

- You define an object that inherits from `cask.MainRoutes`

- Define endpoints using annotated functions, using `@cask.get` or `@cask.post`
  with the route they should match

- Each function can return the data you want in the response, or a
  `cask.Response` if you want further customization: response code, headers,
  etc.

- Your function can take an optional `cask.Request`, which exposes the entire
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

$$$minimalApplication2

You can split up your routes into separate `cask.Routes` objects as makes sense
and pass them all into `cask.Main`.

## Variable Routes

$$$variableRoutes

You can bind path segments to endpoint parameters by declaring them as parameters. These are
either:

* A parameter of the same name as the variable path segment of the same name as you 
  (e.g. `:userName` above). This can be a `String,` or other primitive types like `Int`,
  `Boolean`, `Byte`, `Short`, `Long`, `Float`, `Double`
* A parameter of type `segments: cask.RemainingPathSegments`, if you want to allow 
  the endpoint to handle arbitrary sub-paths of the given path

## Query Params

$$$queryParams

You can bind query parameters to your endpoint method via parameters of the form:

* `param: String` to match `?param=hello`
* `param: Int` for `?param=123`. Other valid types include `Boolean`, `Byte`, `Short`, `Long`, 
  `Float`, `Double`
* `param: Option[T] = None` or `param: String = "DEFAULT VALUE"` for cases where the
  `?param=hello` is optional.
* `param: Seq[T]` for repeated params such as `?param=hello&param=world` with at 
  least one value
* `param: Seq[T] = Nil` for repeated params such as `?param=hello&param=world` allowing
  zero values
* `params: cask.QueryParams` if you want your route to be able to handle arbitrary
  query params without needing to list them out as separate arguments

* `request: cask.Request` which provides lower level access to the things that the HTTP
  request provides

## Multi-method Routes

$$$httpMethods

Sometimes, you may want to handle multiple kinds of HTTP requests in the same
endpoint function, e.g. with code that can accept both GETs and POSTs and decide
what to do in each case. You can use the `@cask.route` annotation to do so

## Receiving Form-encoded or JSON data

$$$formJsonPost

If you need to handle a JSON-encoded POST request, you can use the
`@cask.postJson` decorator. This assumes the posted request body is a
JSON dict, and uses its keys to populate the endpoint's parameters,
either as raw `ujson.Value`s or deserialized into `Seq[Int]`s or other
things. Deserialization is handled using the
[uPickle](https://github.com/lihaoyi/upickle) JSON library, though you
could write your own version of `postJson` to work with any other JSON
library of your choice.

Similarly, you can mark endpoints as `@cask.postForm`, in which case the
endpoints params will be taken from the form-encoded POST body either raw (as
`cask.FormValue`s) or deserialized into simple data structures. Use
`cask.FormFile` if you want the given form value to be a file upload.

Both normal forms and multipart forms are handled the same way.

If the necessary keys are not present in the JSON/form-encoded POST body, or the
deserialization into Scala data-types fails, a 400 response is returned
automatically with a helpful error message.


## Processing Cookies

$$$cookies

Cookies are most easily read by declaring a `: cask.Cookie` parameter; the
parameter name is used to fetch the cookie you are interested in. Cookies can be
stored by setting the `cookie` attribute in the response, and deleted simply by
setting `expires = java.time.Instant.EPOCH` (i.e. to have expired a long time
ago)

## Serving Static Files

$$$staticFiles

You can ask Cask to serve static files by defining a `@cask.staticFiles` endpoint.
This will match any subpath of the value returned by the endpoint (e.g. above
`/static/file.txt`, `/static/folder/file.txt`, etc.) and return the file
contents from the corresponding file on disk (and 404 otherwise).

Similarly, `@cask.staticResources` attempts to serve a request based on the JVM
resource path, returning the data if a resource is present and a 404 otherwise.

You can also configure the `headers` you wish to return to static file requests,
or use `@cask.decorators.compress` to compress the responses:

$$$staticFiles2

## Redirects or Aborts

$$$redirectAbort

Cask provides some convenient helpers `cask.Redirect` and `cask.Abort` which you
can return; these are simple wrappers around `cask.Request`, and simply set up
the relevant headers or status code for you.

## HTML Rendering

Cask doesn't come bundled with HTML templating functionality, but it makes it
really easy to use community-standard libraries like
[Scalatags](https://github.com/lihaoyi/scalatags) to render your HTML. Simply
adding the relevant `ivy"com.lihaoyi::scalatags:0.9.1"` dependency to your
`build.sc` file is enough to render Scalatags templates:

$$$scalatags

If you prefer to use the
[Twirl](https://www.playframework.com/documentation/2.6.x/ScalaTemplates)
templating engine, you can use that too:

$$$twirl

With the following `app/views/hello.scala.html`:
```html
@(titleTxt: String)
<html>
    <body>
        <h1>@titleTxt</h1>
        <p>I am cow</p>
    </body>
</html>
```
## Extending Endpoints with Decorators


$$$decorated

You can write extra decorator annotations that stack on top of the existing
`@cask.get`/`@cask.post` to provide additional arguments or validation. This is
done by implementing the `cask.Decorator` interface and it's `getRawParams`
function. `getRawParams`:

- Receives a `Request`, which basically gives you full access to the
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

For decorators that you wish to apply to multiple routes at once, you can define
them by overriding the `cask.Routes#decorators` field (to apply to every
endpoint in that routes object) or `cask.Main#mainDecorators` (to apply to every
endpoint, period):

$$$decorated2

This is convenient for cases where you want a set of decorators to apply broadly
across your web application, and do not want to repeat them over and over at
every single endpoint.

## Custom Endpoints

$$$endpoints

When you need more flexibility than decorators allow, you can define your own
custom `cask.Endpoint`s to replace the default set that Cask provides. This
allows you to

- Change the expected return type of the annotated function, and how allows you
  to that type gets processed: the above example trivially expects an allows you
  to `Int` which becomes the status code, but you could make it e.g.
  automatically serialize returned objects to JSON responses via your favorite
  library, or serialize them to bytes via protobufs

- Change where the first parameter list's params are taken from: `@cask.get`
  takes them from query params, `@cask.postForm` takes them from the
  form-encoded POST body, and you can write your own endpoint to take the params
  from where-ever you like: perhaps from the request headers, or a protobuf-
  encoded request body

- Change how parameters are deserialized: e.g. `@cask.postJson` de-serializes
  parameters using the [uPickle](https://github.com/lihaoyi/upickle) JSON
  library, and your own custom endpoint could change that to use another library
  like [Circe](https://github.com/circe/circe) or
  [Jackson](https://github.com/FasterXML/jackson-module-scala)

- DRY up common sets of decorators: if all your endpoint functions use the same
  decorators, you can extract that functionality into a single `cask.Endpoint`
  to do the job.

Generally you should not be writing custom `cask.Endpoint`s every day, but if
you find yourself trying to standardize on a way of doing things across your web
application, it might make sense to write a custom endpoint decorator: to DRY
things up , separate business logic (inside the annotated function) from
plumbing (in the endpoint function and decorators), and enforcing a standard of
how endpoint functions are written.

## Gzip & Deflated Responses


$$$compress

Cask provides a useful `@cask.decorators.compress` decorator that gzips or
deflates a response body if possible. This is useful if you don't have a proxy
like Nginx or similar in front of your server to perform the compression for
you.

Like all decorators, `@cask.decorators.compress` can be defined on a level of a
set of `cask.Routes`:

$$$compress2

Or globally, in your `cask.Main`:

$$$compress3

## Websockets

$$$websockets

Cask's Websocket endpoints are very similar to Cask's HTTP endpoints. Annotated
with `@cask.websocket` instead of `@cask.get` or `@cask.post`, the primary
difference is that instead of only returning a `cask.Response`, you now have an
option of returning a `cask.WsHandler`.

The `cask.WsHandler` allows you to pro-actively start sending websocket messages
once a connection has been made, via the `channel: WsChannelActor` it exposes,
and lets you react to messages via the `cask.WsActor` you create. You can use
these two APIs to perform full bi-directional, asynchronous communications, as
websockets are intended to be used for. Note that all messages received on a
each individual Websocket connection by your `cask.WsActor` are handled in a
single-threaded fashion by default: this means you can work with local mutable
state in your `@cask.websocket` endpoint without worrying about race conditions
or multithreading. If you want further parallelism, you can explicitly spin off
`scala.concurrent.Future`s or other `cask.BatchActor`s to perform that parallel
processing.

Returning a `cask.Response` immediately closes the websocket connection, and is
useful if you want to e.g. return a 404 or 403 due to the initial request being
invalid.

Cask also provides a lower-lever websocket interface, which allows you directly
work with the underlying `io.undertow.websockets.WebSocketConnectionCallback`:

$$$websockets2

It leaves it up to you to manage open channels, react to incoming messages, or
pro-actively send them out, mostly using the underlying Undertow webserver
interface. While Cask does not model streams, backpressure, iteratees, or
provide any higher level API, it should not be difficult to take the Cask API
and build whatever higher-level abstractions you prefer to use.

If you are separating your `cask.Routes` from your `cask.Main`, you need to
inject in a `cask.Logger` to handle errors reported when handling websocket
requests:

$$$websockets3

## TodoMVC Api Server


$$$todoApi

This is a simple self-contained example of using Cask to write an in-memory API
server for the common [TodoMVC example app](http://todomvc.com/).

This minimal example intentionally does not contain javascript, HTML, styles,
etc.. Those can be managed via the normal mechanism for
[Serving Static Files](#serving-static-files).

## TodoMVC Database Integration

$$$todoDb

This example demonstrates how to use Cask to write a TodoMVC API server that
persists it's state in a database rather than in memory. We use the
[ScalaSql](https://github.com/com-lihaoyi/scalasql/) database access library to write a `@transactional`
decorator that automatically opens one transaction per call to an endpoint,
ensuring that database queries are properly committed on success or rolled-back
on error. Note that because the default database connector propagates its
transaction context in a thread-local, `@transactional` does not need to pass
the `ctx` object into each endpoint as an additional parameter list, and so we
simply leave it out.

While this example is specific to ScalaSql, you can easily modify the
`@transactional` decorator to make it work with whatever database access library
you happen to be using. For libraries which need an implicit transaction, it can
be passed into each endpoint function as an additional parameter list as
described in
[Extending Endpoints with Decorators](#extending-endpoints-with-decorators).
work with whatever database access library
you happen to be using. For libraries which need an implicit transaction, it can
be passed into each endpoint function as an additional parameter list as
described in
[Extending Endpoints with Decorators](#extending-endpoints-with-decorators).

## TodoMVC Full Stack Web


The following code snippet is the complete code for a full-stack TodoMVC
implementation: including HTML generation for the web UI via
[Scalatags](https://github.com/lihaoyi/scalatags), Javascript for the
interactivity, static file serving, and database integration via
[ScalaSql](https://github.com/com-lihaoyi/scalasql/). While slightly long, this example
should give you a tour of all the things you need to know to use Cask.

Note that this is a "boring" server-side-rendered webapp with Ajax interactions,
without any complex front-end frameworks or libraries: it's purpose is to
demonstrate a simple working web application of using Cask end-to-end, which you
can build upon to create your own Cask web application architected however you
would like.

$$$todo

## Running Cask with Virtual Threads


$$minimalApplicationWithLoom

Cask can support using Virtual Threads to handle the request out of the box, you can enable it with the next steps:

1. Running cask with Java 21 or later
2. add `--add-opens java.base/java.lang=ALL-UNNAMED` to your JVM options, which is needed to name the virtual threads.
3. add `-Dcask.virtual-threads.enabled=true` to your JVM options, which is needed to enable the virtual threads.
4. tweak the underlying carrier threads with `-Djdk.virtualThreadScheduler.parallelism`, `jdk.virtualThreadScheduler.maxPoolSize` and `jdk.unparker.maxPoolSize`.

**Advanced Features**:
1. You can change the default scheduler of the carrier threads with `cask.internal.Util.createVirtualThreadExecutor` method, but keep in mind, that's not officially supported by JDK for now.
2. You can supply your own `Executor` by override the `handlerExecutor()` method in your `cask.Main` object, which will be called only once when the server starts.
3. You can use `jdk.internal.misc.Blocker`'s `begin` and `end` methods to help the `ForkJoinPool` when needed.

**NOTE**: 
1. If your code is CPU-bound, you should not use virtual threads, because it will not improve the performance, but will increase the overhead.
2. A common deadlock can happen when both a virtual thread and a platform thread try to load the same class, but there is no carrier thread available to execute the virtual thread, which will lead to a deadlock, make sure `jdk.unparker.maxPoolSize` is large enough to avoid it.
3. Virtual thread does some kind of `rescheduling` which may lead to higher latency when the task is actually cpu bound.
4. OOM is a common issue when you have many virtual threads, you should limit the max in-flight requests to avoid it.
5. There are some known issues which can leads to a deadlock, you should be careful when using it in production, at least after long time stress test. 
6. [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491) will be shipped in Java 24.
7. Some info from early adaptor [faire](https://craft.faire.com/java-virtual-threads-increasing-search-performance-while-avoiding-deadlocks-f12fa296d521)

**Some benchmark results**:

| Test Case | Thread Type | Requests/sec | Avg Latency | Max Latency | Transfer/sec |
|-----------|-------------|--------------|-------------|-------------|--------------|
| staticFilesWithLoom | Platform | 81,766.27 | 1.23ms | 28.64ms | 11.38MB |
| staticFilesWithLoom | Virtual | 75,488.32 | 4.41ms | 157.91ms | 10.51MB |
| todoDbWithLoom | Platform | 81,929.40 | 1.22ms | 11.67ms | 13.13MB |
| todoDbWithLoom | Virtual | 76,072.80 | 3.97ms | 160.29ms | 12.19MB |
| minimalApplicationWithLoom | Platform | 38.36 | 1.05s | 1.99s | 5.73KB |
| minimalApplicationWithLoom | Virtual | 935.74 | 106.11ms | 126.59ms | 139.81KB |

[data source](https://github.com/com-lihaoyi/cask/pull/159)

*Minimal Application (minimalApplicationWithLoom)*
  - Virtual threads show significantly better performance:
    - ~24x higher throughput (935.74 vs 38.36 requests/sec)
    - Much better latency (106.11ms vs 1.05s)
    - Platform threads experienced 1,076 timeout errors while virtual threads had none
