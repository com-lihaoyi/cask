package app
object FormJsonPost extends cask.MainRoutes{
  @cask.postJson("/json")
  def jsonEndpoint(value1: ujson.Value, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postForm("/form")
  def formEndpoint(value1: cask.FormValue, value2: Seq[Int]) = {
    "OK " + value1 + " " + value2
  }

  @cask.postForm("/upload")
  def uploadFile(image: cask.FormFile) = {
    image.fileName
  }

  initialize()
}
