import mill._, scalalib._

object cask extends ScalaModule{
  def scalaVersion = "2.12.6"
  def ivyDeps = Agg(
    ivy"org.scala-lang:scala-reflect:$scalaVersion",
    ivy"io.undertow:undertow-core:2.0.11.Final",
    ivy"com.github.scopt::scopt:3.5.0"
  )
  object test extends Tests{
    def forkArgs = Seq("--illegal-access=deny")
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.6.3")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
