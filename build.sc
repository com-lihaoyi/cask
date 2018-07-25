import mill._, scalalib._

object cask extends ScalaModule{
  def scalaVersion = "2.12.6"
  def ivyDeps = Agg(
    ivy"org.scala-lang:scala-reflect:$scalaVersion",
    ivy"io.undertow:undertow-core:2.0.11.Final",
    ivy"com.github.scopt::scopt:3.5.0",
    ivy"com.lihaoyi::upickle:0.6.6",
    ivy"com.lihaoyi::scalatags:0.6.7",
    ivy"com.lihaoyi::fastparse:1.0.0",
    ivy"com.lihaoyi::pprint:0.5.3",
  )
  def compileIvyDeps = Agg(ivy"com.lihaoyi::acyclic:0.1.7")
  def scalacOptions = Seq("-P:acyclic:force")
  def scalacPluginIvyDeps = Agg(ivy"com.lihaoyi::acyclic:0.1.7")

  object test extends Tests{
    def forkArgs = Seq("--illegal-access=deny")
    def testFrameworks = Seq("utest.runner.Framework")
    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.6.3",
      ivy"com.lihaoyi::requests::0.1.2"
    )
  }
}
