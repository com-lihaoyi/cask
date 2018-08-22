import mill._, scalalib._


trait AppModule extends ScalaModule{
  def scalaVersion = "2.12.6"
  def ivyDeps = Agg(
    ivy"com.lihaoyi::cask:0.1.1",
  )

  def forkWorkingDir = build.millSourcePath

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.6.3",
      ivy"com.lihaoyi::requests::0.1.3",
    )

    def forkWorkingDir = build.millSourcePath

    // redirect this to the forked `test` to make sure static file serving works
    def testLocal(args: String*) = T.command{
      test(args:_*)
    }
  }
}