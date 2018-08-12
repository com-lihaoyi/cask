package app
object HttpMethods extends cask.MainRoutes{
  @cask.route("/login", methods = Seq("get", "post"))
  def login(request: cask.Request) = {
    if (request.exchange.getRequestMethod.equalToString("post")) "do_the_login"
    else "show_the_login_form"
  }

  initialize()
}
