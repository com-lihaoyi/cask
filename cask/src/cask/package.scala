package object cask {
  def redirect(url: String) = Response(
    "",
    301,
    headers = Seq("Location" -> url)
  )
  def abort(code: Int) = Response(
    "",
    code
  )
}
