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

  initialize()
}
