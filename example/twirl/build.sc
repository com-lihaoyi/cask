import mill._, scalalib._
import $ivy.`com.lihaoyi::mill-contrib-twirllib:$MILL_VERSION`

trait AppModule extends ScalaModule with mill.twirllib.TwirlModule{
  def scalaVersion = "2.13.2"
  def twirlVersion = "1.5.0-M1"

  def generatedSources = T{ Seq(compileTwirl().classes) }
  def ivyDeps = Agg[Dep](
    ivy"com.lihaoyi::scalatags:0.9.1",
    ivy"com.typesafe.play::twirl-api:${twirlVersion()}",
  )

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.4",
      ivy"com.lihaoyi::requests::0.6.2",
    )
  }
}