import mill._, scalalib._


trait AppModule extends ScalaModule{
  def scalaVersion = "2.13.0"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::cask:0.2.1",
    ivy"org.xerial:sqlite-jdbc:3.18.0",
    ivy"io.getquill::quill-jdbc:2.5.4",
    ivy"com.lihaoyi::scalatags:0.7.0",
  )

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.6.9",
      ivy"com.lihaoyi::requests::0.2.0",
    )
  }
}