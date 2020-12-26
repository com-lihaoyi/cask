import mill._, scalalib._


trait AppModule extends CrossScalaModule{


  def forkWorkingDir = build.millSourcePath
  def ivyDeps = Agg[Dep](
  )
  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.7",
      ivy"com.lihaoyi::requests::0.6.5",
    )

    def forkWorkingDir = build.millSourcePath

    // redirect this to the forked `test` to make sure static file serving works
    def testLocal(args: String*) = T.command{
      test(args:_*)
    }
  }
}
