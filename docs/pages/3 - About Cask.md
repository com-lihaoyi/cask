
## Functions First

Inspired by [Flask](http://flask.pocoo.org/), Cask allows you to define your web
applications endpoints using simple function `def`s that you already know and
love, annotated with the minimal additional metadata necessary to work as HTTP
endpoints.

It turns out that function `def`s already provide almost everything you need in
a HTTP endpoint:

- The parameters the endpoint takes
- If any parameters are optional, and their default values
- The ability to return a `Response`

Cask extends these basics with annotations, providing:

- What request path the endpoint is available at
- Automated deserialization of endpoint parameters from the respective format
  (Form-encoded? Query-string? JSON?)
- Wrapping the endpoint's function `def` with custom logic: logging,
  authentication, ...

While these annotations add a bit of complexity, they allow Cask to avoid
needing custom DSLs for defining your HTTP routes, custom action-types, and many
other things which you may be used to working with HTTP in Scala.

## Extensible Annotations

Unlike most other annotation-based frameworks in Scala or Java, Cask's
annotations are not magic markers, but self-contained classes containing all the
logic they need to function. This has several benefits:

- You can jump to the definition of an annotation and see what it does

- It trivial to implement your own annotations as
  [decorators](/cask#extending-endpoints-with-decorators) or
  [endpoints](/cask#custom-endpoints).

- Stacking multiple annotations on a single function has a well-defined contract
  and semantics

Overall, Cask annotations behave a lot more like Python decorators than
"traditional" Java/Scala annotations: first-class, customizable, inspectable,
and self-contained. This allows Cask to have the syntactic convenience of an
annotation-based API, without the typical downsides of inflexibility and
undiscoverability.

## Simple First

Cask intentionally eskews many things that other, more enterprise-grade
frameworks provide:

- Async
- Akka
- Streaming Computations
- Backpressure

While these features all are valuable in specific cases, Cask aims for the 99%
of code for which simple, boring code is perfectly fine. Cask's endpoints are
synchronous by default, do not tie you to any underlying concurrency model, and
should "just work" without any advanced knowledge apart from basic Scala and
HTTP. Cask's [websockets](/cask#websockets) API is intentionally low-level, making it
both simple to use and also simple to build on top of if you want to wrap it in
your own concurrency-library-of-choice.

## Thin Wrapper

Cask is implemented as a thin wrapper around the excellent Undertow HTTP server.
If you need more advanced functionality, Cask lets you ask for the `exchange:
HttpServerExchange` in your endpoint, override
[defaultHandler](/cask#def-defaulthandler) and add your own Undertow handlers next to
Cask's and avoid Cask's routing/endpoint system altogether, or override
[main](/cask#def-main) if you want to change how the server is initialized.

Rather than trying to provide APIs for all conceivable functionality, Cask
simply provides what it does best - simple routing for simple endpoints - and
leaves the door wide open in case you need to drop down to the lower level
Undertow APIs.

## Community Libraries

Cask aims to re-use much of the excellent code that is already written and being
used out in the Scala community, rather than trying to re-invent the wheel. Cask
uses the [Mill](https://github.com/lihaoyi/mill) build tool, comes bundled with
the [uPickle](https://github.com/lihaoyi/upickle) JSON library, and makes it
trivial to pull in libraries like
[Scalatags](https://github.com/lihaoyi/scalatags) to render HTML or
[ScalaSql](https://github.com/com-lihaoyi/scalasql/) for database access.

Each of these are stable, well-known, well-documented libraries you may already
be familiar with, and Cask simply provides the HTTP/routing layer with the hooks
necessary to tie everything together (e.g. into a
[TodoMVC](/cask#todomvc-full-stack-web) webapp)