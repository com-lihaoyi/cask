package app
object QueryParams extends cask.MainRoutes{

  @cask.get("/article/:articleId") // Mandatory query param
  def getArticle(articleId: Int, param: String) = { 
    s"Article $articleId $param"
  }

  @cask.get("/article2/:articleId") // Optional query param
  def getArticleOptional(articleId: Int, param: Option[String] = None) = {
    s"Article $articleId $param"
  }

  @cask.get("/article3/:articleId") // Optional query param with default
  def getArticleDefault(articleId: Int, param: String = "DEFAULT VALUE") = {
    s"Article $articleId $param"
  }

  @cask.get("/article4/:articleId") // 1-or-more query param
  def getArticleSeq(articleId: Int, param: Seq[String]) = {
    s"Article $articleId $param"
  }

  @cask.get("/article5/:articleId") // 0-or-more query param
  def getArticleOptionalSeq(articleId: Int, param: Seq[String] = Nil) = {
    s"Article $articleId $param"
  }

  @cask.get("/user2/:userName") // allow unknown query params
  def getUserProfileAllowUnknown(userName: String, params: cask.QueryParams) = {
    s"User $userName " + params.value
  }

  initialize()
}
