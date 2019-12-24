import mill._, scalalib._


trait AppModule extends ScalaModule{
  def scalaVersion = "2.13.0"
  def ivyDeps = Agg[Dep](
  )
  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.1",
      ivy"com.lihaoyi::requests::0.4.4"
    )
  }
}