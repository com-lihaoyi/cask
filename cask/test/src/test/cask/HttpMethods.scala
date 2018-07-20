package test.cask

import io.undertow.server.HttpServerExchange

object HttpMethods extends cask.MainRoutes{
  @cask.route("/login", methods = Seq("GET", "POST"))
  def login(exchange: HttpServerExchange) = {
    if (exchange.getRequestMethod.equalToString("POST")) "do_the_login"
    else "show_the_login_form"
  }

  initialize()
}
