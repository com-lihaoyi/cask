Apart from the code used to configure and define your routes and endpoints, Cask
also allows global configuration for things that apply to the entire web server.
This can be done by overriding the following methods on `cask.Main` or
`cask.MainRoutes`:

## def debugMode: Boolean = true

Makes the Cask report verbose error messages and stack traces if an endpoint
fails; useful for debugging, should be disabled for production.

## def main

The cask program entrypoint. By default just spins up a webserver, but you can
override it to do whatever you like before or after the webserver runs.

## def log

A logger that gets passed around the application. Used for convenient debug
logging, as well as logging exceptions either to the terminal or to a
centralized exception handler.

## def defaultHandler

Cask is built on top of the [Undertow](http://undertow.io/) web server. If you
need some low-level functionality not exposed by the Cask API, you can override
`defaultHandler` to make use of Undertow's own
[handler API](http://undertow.io/undertow-docs/undertow-docs-2.0.0/index.html#built-in-handlers)
for customizing your webserver. This allows for things that Cask itself doesn't
internally support.

## def port: Int = 8080, def host: String = "localhost"

The host & port to attach your webserver to.

## def handleNotFound

The response to serve when the incoming request does not match any of the routes
or endpoints; defaults to a typical 404

## def handleEndpointError

The response to serve when the incoming request matches a route and endpoint,
but then fails for other reasons. Defaults to 400 for mismatched or invalid
endpoint arguments and 500 for exceptions in the endpoint body, and provides
useful stack traces or metadata for debugging if `debugMode = true`.

## def mainDecorators

Any `cask.Decorator`s that you want to apply to all routes and all endpoints in
the entire web application. Useful for inserting application-wide
instrumentation, logging, security-checks, and similar things.
