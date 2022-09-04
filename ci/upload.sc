#!/usr/bin/env amm

import $ivy.`com.lihaoyi::os-lib:0.8.1`

@main
def apply(uploadedFile: os.Path,
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
    data = os.read.bytes(uploadedFile),
    headers = Seq(
      "Content-Type" -> "application/octet-stream",
      "Authorization" -> s"token $authKey"
    ),
    connectTimeout = 5000, readTimeout = 60000
  )


  println(res.text)
  val longUrl = ujson.read(res.text)("browser_download_url").str.toString

  longUrl
}
