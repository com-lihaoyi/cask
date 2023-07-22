import mill._, scalalib._

trait AppModule extends CrossScalaModule{

  def ivyDeps = Agg[Dep](
    ivy"org.xerial:sqlite-jdbc:3.42.0.0",
    ivy"io.getquill::quill-jdbc:4.6.1",
    ivy"org.slf4j:slf4j-simple:1.7.30"
  )

  object test extends ScalaTests with TestModule.Utest{

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.8.1",
      ivy"com.lihaoyi::requests::0.8.0",
    )
  }
}
