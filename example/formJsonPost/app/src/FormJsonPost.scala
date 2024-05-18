package app
object FormJsonPost extends cask.MainRoutes{
  @cask.postJson("/json")
  def jsonEndpoint(value1: ujson.Value, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postJson("/json-obj")
  def jsonEndpointObj(value1: ujson.Value, value2: Seq[Int]) = {
    ujson.Obj(
      "value1" -> value1,
      "value2" -> value2
    )
  }

  @cask.postJsonCached("/json-obj-cached")
  def jsonEndpointObjCached(value1: ujson.Value, value2: Seq[Int], request: cask.Request) = {
    ujson.Obj(
      "value1" -> value1,
      "value2" -> value2,
      // `cacheBody = true` buffers up the body of the request in memory before parsing,
      // giving you access to the request body data if you want to use it yourself
      "body" -> request.text()
    )
  }

  @cask.postForm("/form")
  def formEndpoint(value1: cask.FormValue, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postForm("/form-obj")
  def formEndpointObj(value1: cask.FormValue, value2: Seq[Int]) = {
    ujson.Obj(
      "value1" -> value1.value,
      "value2" -> value2
    )
  }

  @cask.postForm("/upload")
  def uploadFile(image: cask.FormFile) = {
    image.fileName
  }


  @cask.postJson("/json-extra")
  def jsonEndpointExtra(value1: ujson.Value,
                        value2: Seq[Int],
                        params: cask.QueryParams,
                        segments: cask.RemainingPathSegments) = {
    "OK " + value1 + " " + value2 + " " + params.value + " " + segments.value
  }

  @cask.postForm("/form-extra")
  def formEndpointExtra(value1: cask.FormValue, 
                        value2: Seq[Int],
                        params: cask.QueryParams,
                        segments: cask.RemainingPathSegments) = {
    "OK " + value1 + " " + value2 + " " + params.value + " " + segments.value
  }
  
  initialize()
}
