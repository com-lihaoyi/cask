Cask: a Scala HTTP micro-framework [![Build Status][travis-badge]][travis-link] [![Gitter Chat][gitter-badge]][gitter-link] [![Patreon][patreon-badge]][patreon-link]
=====================================================================================================================================================================

[travis-badge]: https://travis-ci.org/lihaoyi/cask.svg
[travis-link]: https://travis-ci.org/lihaoyi/cask
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/lihaoyi/cask?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[patreon-badge]: https://img.shields.io/badge/patreon-sponsor-ff69b4.svg
[patreon-link]: https://www.patreon.com/lihaoyi

```scala
object MinimalApplication extends cask.MainRoutes{
  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.post("/do-thing")
  def doThing(request: cask.Request) = {
    request.text().reverse
  }

  initialize()
}
```

Cask is a simple Scala web framework inspired by Python's
[Flask](http://flask.pocoo.org/docs/1.0/) project. It aims to bring simplicity,
flexibility and ease-of-use to Scala webservers, avoiding cryptic DSLs or
complicated asynchrony.

- [Documentation](https://com-lihaoyi.github.io/cask/)

If you use Cask and like it, you will probably enjoy the following book by the Author:

- [*Hands-on Scala Programming*](https://www.handsonscala.com/)

*Hands-on Scala* has uses Requests-Scala extensively throughout the book, and has
the entirety of *Chapter 14: Simple Web and API Servers* dedicated to
the library. *Hands-on Scala* is a great way to level up your skills in Scala
in general and Cask in particular.

Cask is profiled using the
[JProfiler Java Profiler](https://www.ej-technologies.com/products/jprofiler/overview.html),
courtesy of EJ Technologies

## Changelog

### 0.7.9

- Add support for Scala 3.0.0-RC2

### 0.7.8

- Upgrade undertow
- Add support for Scala 3.0.0-M2

### 0.7.7

- Fix published examples

### 0.7.6

- Add support for Dotty (to be Scala 3)

### 0.7.4

- Bump Mill version to 0.8.0

### 0.7.3

- Make Cask `actorContext` explicitly passed into every Routes case class

### 0.6.5

- Add support for `geny.Writable#httpContentType` and
  `geny.Writable#contentLength`

### 0.5.7

- Add endpoints for `delete` and `patch`
- Allow arbitrary HTTP methods

### 0.5.2

- Bump uPickle, Requests versions

### 0.3.7

- Add `SameSite` cookie attribute
- Fix bug in default parameters of cask routes

### 0.3.6

- Extract `cask-actor` into its own repo and artifact,
  [https://github.com/lihaoyi/castor](https://github.com/lihaoyi/castor)

### 0.3.3

- Separate `cask-actor` into a separate artifact, documented separately as
  [Cask Actors](http://www.lihaoyi.com/cask/page/cask-actors.html)

### 0.3.2

- Support for Scala 2.13.2

### 0.3.1

- Mismatched decorator types applied to a single method is now a compile error

- `staticFiles` and `staticResources` now allows you to specify response headers

- Allow `cask.decorators.compress` to be used as a `cask.Routes` or `cask.Main`
  decorator without crashing on websocket responses

- Allow decorators to be defined and used for non-`cask.Response` results

### 0.3.0

- Fix crashes in `cask.WebsocketClientImpl`

### 0.2.9

- Provide a simple cross-platform builtin websocket client in `cask.WsClient`

### 0.2.8

- Make `Routes#log` implicit

### 0.2.7

- Cross-publish `cask.util` for Scala.js

### 0.2.6

- Embed `concurrent.ExecutionContext.global` in `cask.Routes` by default, to be
  overriden if necessary

### 0.2.5

- Internal refactoring to clean up routing logic

### 0.2.4

- Standardize on a basic `cask.Logger` interface
- Create a simple actor-based API for handling websockets in `cask.WsHandler`
  and `cask.WsActor`

### 0.2.3

- `cask.Response` is now covariant

### 0.2.2

- Use standard `./mill` bootstrap script

### 0.2.1

- Support for Scala 2.13.0

