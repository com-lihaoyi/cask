import mill._, scalalib._
import $ivy.`com.lihaoyi::mill-contrib-twirllib:`

trait AppModule extends CrossScalaModule with mill.twirllib.TwirlModule{

  def twirlScalaVersion = "2.13.10"
  def twirlVersion = "1.5.1"

  def generatedSources = T{ Seq(compileTwirl().classes) }
  def ivyDeps = Agg[Dep](
    ivy"com.lihaoyi::scalatags:0.9.1".withDottyCompat(scalaVersion()),
    ivy"com.typesafe.play::twirl-api:${twirlVersion()}".withDottyCompat(scalaVersion()),
  )

  object test extends ScalaTests with TestModule.Utest{

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.8.1",
      ivy"com.lihaoyi::requests::0.8.0",
    )
  }
}
