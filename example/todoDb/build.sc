import mill._, scalalib._

trait AppModule extends CrossScalaModule{

  def ivyDeps = Agg[Dep](
    ivy"org.xerial:sqlite-jdbc:3.42.0.0",
    ivy"com.lihaoyi::scalasql:0.1.0",
  )

  object test extends ScalaTests with TestModule.Utest{

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.8.1",
      ivy"com.lihaoyi::requests::0.8.0",
    )
  }
}
