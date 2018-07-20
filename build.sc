import mill._, scalalib._

object cask extends ScalaModule{
  def scalaVersion = "2.12.6"
  def ivyDeps = Agg(ivy"org.scala-lang:scala-reflect:$scalaVersion")
  object test extends Tests{
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.6.3")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
