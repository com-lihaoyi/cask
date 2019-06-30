import mill._, scalalib._


trait AppModule extends ScalaModule{
  def scalaVersion = "2.13.0"
  def ivyDeps = Agg[Dep](
    ivy"org.xerial:sqlite-jdbc:3.18.0",
    ivy"io.getquill::quill-jdbc:2.5.4"
  )

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.6.9",
      ivy"com.lihaoyi::requests::0.2.0",
    )
  }
}