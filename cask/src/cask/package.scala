package object cask {
  // model
  type Response = model.Response
  val Response = model.Response
  type Abort = model.Abort
  val Abort = model.Abort
  type Redirect = model.Redirect
  val Redirect = model.Redirect
  type Cookie = model.Cookie
  val Cookie = model.Cookie

  // endpoints
  type get = endpoints.get
  type post = endpoints.post
  type put = endpoints.put
  type route = endpoints.route
  type static = endpoints.static
  type postJson = endpoints.postJson
  type postForm = endpoints.postForm

  // endpoints misc
  type FormValue = endpoints.FormValue
  val FormValue = endpoints.FormValue
  type Cookies = endpoints.Cookies
  val Cookies = endpoints.Cookies
  type Subpath = endpoints.Subpath
  val Subpath = endpoints.Subpath
  type CookieParam = endpoints.CookieParam
  val CookieParam = endpoints.CookieParam

  // main
  type MainRoutes = main.MainRoutes
  type Routes = main.Routes
  type Main = main.Main

}
