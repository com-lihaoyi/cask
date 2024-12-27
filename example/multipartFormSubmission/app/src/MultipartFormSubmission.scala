package app

object MultipartFormSubmission extends cask.MainRoutes {

  @cask.get("/")
  def index() =
    cask.model.Response(
      """
    <!DOCTYPE html>
    <html lang="en">
    <head></head>
    <body>
        <form action="/post" method="post" enctype="multipart/form-data">
            <input type="file" id="somefile" name="somefile">
            <button type="submit">Submit</button>
        </form>
    </body>
    </html>
    """, 200, Seq(("Content-Type", "text/html")))

  @cask.postForm("/post")
  def post(somefile: cask.FormFile) =
    s"filename: ${somefile.fileName}"

  initialize()
}
