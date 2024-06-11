import mill._, scalalib._

trait AppModule extends CrossScalaModule{

  def ivyDeps = Agg[Dep](
    ivy"com.lihaoyi::scalatags:0.12.0"
  )

  object test extends ScalaTests with TestModule.Utest{

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.8.3",
      ivy"com.lihaoyi::requests::0.8.3",
    )
  }
}
