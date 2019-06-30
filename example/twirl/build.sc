import mill._, scalalib._
import $ivy.`com.lihaoyi::mill-contrib-twirllib:0.4.1-4-158d11`

trait AppModule extends ScalaModule with mill.twirllib.TwirlModule{
  def scalaVersion = "2.13.0"
  def twirlVersion = "1.5.0-M1"

  def generatedSources = T{ Seq(compileTwirl().classes) }
  def ivyDeps = Agg(
    ivy"com.lihaoyi::cask:0.2.1",
    ivy"com.lihaoyi::scalatags:0.7.0",
    ivy"com.typesafe.play::twirl-api:${twirlVersion()}",
  )

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.6.9",
      ivy"com.lihaoyi::requests::0.2.0",
    )
  }
}