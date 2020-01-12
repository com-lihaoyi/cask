import mill._, scalalib._


trait AppModule extends ScalaModule{
  def scalaVersion = "2.13.1"
  def ivyDeps = Agg[Dep](
    ivy"com.lihaoyi::scalatags:0.8.4",
  )

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.3",
      ivy"com.lihaoyi::requests::0.4.9",
    )
  }
}