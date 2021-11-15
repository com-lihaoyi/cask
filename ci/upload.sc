#!/usr/bin/env amm
import ammonite.ops._

@main
def shorten(longUrl: String) = {
  println("shorten longUrl " + longUrl)
  val shortUrl = requests.post(
    "https://git.io",
    data = Seq("url" -> longUrl),
  ).headers("location").head

  println("shorten shortUrl " + shortUrl)
  shortUrl
}
@main
def apply(uploadedFile: Path,
          tagName: String,
          uploadName: String,
          authKey: String): String = {
  val body = requests.get(
    s"https://api.github.com/repos/com-lihaoyi/cask/releases/tags/$tagName",
    headers = Seq("Authorization" -> s"token $authKey")
  ).text

  val parsed = ujson.read(body)

  println(body)

  val snapshotReleaseId = parsed("id").num.toInt


  val uploadUrl =
    s"https://uploads.github.com/repos/com-lihaoyi/cask/releases/" +
      s"$snapshotReleaseId/assets?name=$uploadName"

  val res = requests.post(
    uploadUrl,
    data = read.bytes! uploadedFile,
    headers = Seq(
      "Content-Type" -> "application/octet-stream",
      "Authorization" -> s"token $authKey"
    ),
    connectTimeout = 5000, readTimeout = 60000
  )


  println(res.text)
  val longUrl = ujson.read(res.text)("browser_download_url").str.toString

  println("Long Url " + longUrl)

  val shortUrl = shorten(longUrl)

  println("Short Url " + shortUrl)
  shortUrl
}
