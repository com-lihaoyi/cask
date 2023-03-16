Cask 0.9.0: a Scala HTTP micro-framework [![Build Status][travis-badge]][travis-link] [![Gitter Chat][gitter-badge]][gitter-link] [![Patreon][patreon-badge]][patreon-link]
===========================================================================================================================================================================

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

### 0.9.0

- Update uPickle to 3.0.0
- Update Geny to 1.0.0
- Update Scala 3 to 3.2.2
- Update Scala.js to 1.13.0
- Update SourceCode to 0.3.0
- Update PPrint to 0.8.1
- Update Castor to 0.3.0

### 0.8.3

- Fix error reporting for invalid routes [#70](https://github.com/com-lihaoyi/cask/pull/70)

### 0.8.2

- Bump uPickle to 1.6.0

### 0.8.1

- Publish Cask for Scala 2.12 again

### 0.8.0

- Improve handling on 404/405 responses with unsupported methods
  ([#52](https://github.com/com-lihaoyi/cask/pull/52))

### 0.7.21

- Fix example project zips

### 0.7.14

- Update Castor to 0.1.8
- Add `@cask.options` decorator

### 0.7.11

- Build for Scala 3.0.0

### 0.7.10

- Return 405 for unsupported HTTP methods
- Upgrade Scala versions to 2.13.5 and 3.0.0-RC3

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

