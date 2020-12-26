import mill._, scalalib._


trait AppModule extends CrossScalaModule{

  def ivyDeps = Agg[Dep](
    ivy"org.xerial:sqlite-jdbc:3.18.0",
    ivy"io.getquill::quill-jdbc:3.4.10"
  )

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.8",
      ivy"com.lihaoyi::requests::0.6.7",
    )
  }
}
