import mill._, scalalib._
import $ivy.`com.lihaoyi::mill-contrib-twirllib:0.2.6-27-613878`

trait AppModule extends ScalaModule with mill.twirllib.TwirlModule{
  def scalaVersion = "2.12.6"
  def twirlVersion = "1.3.15"

  def generatedSources = T{ Seq(compileTwirl().classes) }
  def ivyDeps = Agg(
    ivy"com.lihaoyi::cask:0.1.1",
    ivy"com.lihaoyi::scalatags:0.6.7",
    ivy"com.typesafe.play::twirl-api:${twirlVersion()}",
  )

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.6.3",
      ivy"com.lihaoyi::requests::0.1.8",
    )
  }
}