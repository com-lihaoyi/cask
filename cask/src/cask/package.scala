package object cask {
  // model
  type Response = model.Response
  val Response = model.Response
  type Abort = model.Abort
  val Abort = model.Abort
  type Redirect = model.Redirect
  val Redirect = model.Redirect
  type FormEntry = model.FormEntry
  val FormEntry = model.FormEntry
  type FormValue = model.FormValue
  val FormValue = model.FormValue
  type FormFile = model.FormFile
  val FormFile = model.FormFile
  type Cookie = model.Cookie
  val Cookie = model.Cookie
  type Subpath = model.Subpath
  val Subpath = model.Subpath
  type Request = model.Request
  val Request = model.Request
  type ParamContext = model.ParamContext
  val ParamContext = model.ParamContext

  // endpoints
  type websocket = endpoints.websocket
  val WebsocketResult = endpoints.WebsocketResult
  type WebsocketResult = endpoints.WebsocketResult

  type get = endpoints.get
  type post = endpoints.post
  type put = endpoints.put
  type route = endpoints.route
  type staticFiles = endpoints.staticFiles
  type staticResources = endpoints.staticResources
  type postJson = endpoints.postJson
  type postForm = endpoints.postForm

  // main
  type MainRoutes = main.MainRoutes
  type Routes = main.Routes
  val Routes = main.Routes
  type Main = main.Main
  type Decorator = main.Decorator
  type Endpoint = main.Endpoint
  type BaseDecorator = main.HttpDecorator

}
