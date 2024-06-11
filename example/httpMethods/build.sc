import mill._, scalalib._

trait AppModule extends CrossScalaModule{

  def ivyDeps = Agg[Dep](
  )
  object test extends ScalaTests with TestModule.Utest{

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.8.3",
      ivy"com.lihaoyi::requests::0.8.3",
    )
    def forkArgs = Seq("--add-opens=java.base/java.net=ALL-UNNAMED")
  }
}
