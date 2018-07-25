package object cask {
  // model
  type Response = model.Response
  val Response = model.Response
  type Abort = model.Abort
  val Abort = model.Abort
  type Redirect = model.Redirect
  val Redirect = model.Redirect
  type FormValue = model.FormValue
  val FormValue = model.FormValue
  type Cookie = model.Cookie
  val Cookie = model.Cookie
  type Cookies = model.Cookies
  type Subpath = model.Subpath

  // endpoints
  type get = endpoints.get
  type post = endpoints.post
  type put = endpoints.put
  type route = endpoints.route
  type static = endpoints.static
  type postJson = endpoints.postJson
  type postForm = endpoints.postForm

  // main
  type MainRoutes = main.MainRoutes
  type Routes = main.Routes
  type Main = main.Main

}
