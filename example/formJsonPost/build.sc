import mill._, scalalib._


trait AppModule extends CrossScalaModule{

  def ivyDeps = Agg[Dep](
  )
  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.8",
      ivy"com.lihaoyi::requests::0.6.7"
    )
  }
}
