package app

object ConfigExample extends cask.MainRoutes {

  // Configuration loaded automatically at startup
  val appName = cask.Config.getStringOrThrow("app.name")
  val debugMode = cask.Config.getBooleanOrThrow("app.features.debug")

  override def port = cask.Config.getIntOrThrow("app.server.port")
  override def host = cask.Config.getStringOrThrow("app.server.host")

  @cask.get("/")
  def index() = {
    val env = cask.Config.Environment.current.name
    s"""
       |App: $appName
       |Environment: $env
       |Debug: $debugMode
       |Port: $port
       |""".stripMargin
  }

  @cask.get("/config/:key")
  def getConfig(key: String) = {
    cask.Config.getString(key) match {
      case Right(value) => s"$key = $value"
      case Left(error) => cask.Response(error.message, statusCode = 404)
    }
  }

  initialize()
}
