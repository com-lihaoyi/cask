import mill._, scalalib._, scalajslib._, publish._

import $file.ci.upload, $file.ci.version
import $file.example.compress.build
import $file.example.compress2.build
import $file.example.compress3.build
import $file.example.cookies.build
import $file.example.decorated.build
import $file.example.decorated2.build
import $file.example.endpoints.build
import $file.example.formJsonPost.build
import $file.example.httpMethods.build
import $file.example.minimalApplication.build
import $file.example.minimalApplication2.build
import $file.example.redirectAbort.build
import $file.example.scalatags.build
import $file.example.staticFiles.build
import $file.example.staticFiles2.build
import $file.example.todo.build
import $file.example.todoApi.build
import $file.example.todoDb.build
import $file.example.twirl.build
import $file.example.variableRoutes.build
import $file.example.websockets.build
import $file.example.websockets2.build
import $file.example.websockets3.build
import $file.example.websockets4.build

trait CaskModule extends ScalaModule with PublishModule{
  def scalaVersion = "2.13.1"

  def publishVersion = build.publishVersion()._2

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/cask",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("lihaoyi", "cask"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
    )
  )
}

object cask extends CaskModule {
  def moduleDeps = Seq(util.jvm)

  def ivyDeps = Agg(
    ivy"org.scala-lang:scala-reflect:${scalaVersion()}",
    ivy"io.undertow:undertow-core:2.0.13.Final",
    ivy"com.lihaoyi::upickle:0.8.0"
  )
  def compileIvyDeps = Agg(ivy"com.lihaoyi::acyclic:0.2.0")
  def scalacOptions = Seq("-P:acyclic:force")
  def scalacPluginIvyDeps = Agg(ivy"com.lihaoyi::acyclic:0.2.0")


  object util extends Module {
    trait UtilModule extends CaskModule {
      def artifactName = "cask-util"
      def platformSegment: String
      def millSourcePath = super.millSourcePath / os.up

      def sources = T.sources(
        millSourcePath / "src",
        millSourcePath / s"src-$platformSegment"
      )
      def ivyDeps = Agg(
        ivy"com.lihaoyi::sourcecode:0.1.8",
        ivy"com.lihaoyi::pprint:0.5.5"
      )
    }

    object js extends UtilModule with ScalaJSModule{
      def platformSegment = "js"
      def scalaJSVersion = "0.6.29"
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::castor::0.1.0",
        ivy"org.scala-js::scalajs-dom::0.9.7"
      )
    }
    object jvm extends UtilModule{
      def platformSegment = "jvm"
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::castor::0.1.0",
        ivy"org.java-websocket:Java-WebSocket:1.4.0"
      )
    }
  }

  object test extends Tests{

    def testFrameworks = Seq("utest.runner.Framework")
    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.1",
      ivy"com.lihaoyi::requests::0.2.0"
    )
  }
}
object example extends Module{
  trait LocalModule extends ScalaModule{
    override def millSourcePath = super.millSourcePath / "app"
    def moduleDeps = Seq(cask)
  }
  object compress extends $file.example.compress.build.AppModule with LocalModule
  object compress2 extends $file.example.compress2.build.AppModule with LocalModule
  object compress3 extends $file.example.compress3.build.AppModule with LocalModule
  object cookies extends $file.example.cookies.build.AppModule with LocalModule
  object decorated extends $file.example.decorated.build.AppModule with LocalModule
  object decorated2 extends $file.example.decorated2.build.AppModule with LocalModule
  object endpoints extends $file.example.endpoints.build.AppModule with LocalModule
  object formJsonPost extends $file.example.formJsonPost.build.AppModule with LocalModule
  object httpMethods extends $file.example.httpMethods.build.AppModule with LocalModule
  object minimalApplication extends $file.example.minimalApplication.build.AppModule with LocalModule
  object minimalApplication2 extends $file.example.minimalApplication2.build.AppModule with LocalModule
  object redirectAbort extends $file.example.redirectAbort.build.AppModule with LocalModule
  object scalatags extends $file.example.scalatags.build.AppModule with LocalModule
  object staticFiles extends $file.example.staticFiles.build.AppModule with LocalModule
  object staticFiles2 extends $file.example.staticFiles2.build.AppModule with LocalModule
  object todo extends $file.example.todo.build.AppModule with LocalModule
  object todoApi extends $file.example.todoApi.build.AppModule with LocalModule
  object todoDb extends $file.example.todoDb.build.AppModule with LocalModule
  object twirl extends $file.example.twirl.build.AppModule with LocalModule
  object variableRoutes extends $file.example.variableRoutes.build.AppModule with LocalModule
  object websockets extends $file.example.websockets.build.AppModule with LocalModule
  object websockets2 extends $file.example.websockets2.build.AppModule with LocalModule
  object websockets3 extends $file.example.websockets3.build.AppModule with LocalModule
  object websockets4 extends $file.example.websockets4.build.AppModule with LocalModule
}

def publishVersion = T.input($file.ci.version.publishVersion)
def gitHead = T.input($file.ci.version.gitHead)

def uploadToGithub(authKey: String) = T.command{
  val (releaseTag, label) = publishVersion()

  if (releaseTag == label){
    requests.post(
      "https://api.github.com/repos/lihaoyi/cask/releases",
      data = ujson.write(
        ujson.Obj(
          "tag_name" -> releaseTag,
          "name" -> releaseTag
        )
      ),
      headers = Seq("Authorization" -> s"token $authKey")
    )
  }

  val examples = Seq(
    $file.example.compress.build.millSourcePath,
    $file.example.compress2.build.millSourcePath,
    $file.example.compress3.build.millSourcePath,
    $file.example.cookies.build.millSourcePath,
    $file.example.decorated.build.millSourcePath,
    $file.example.decorated2.build.millSourcePath,
    $file.example.endpoints.build.millSourcePath,
    $file.example.formJsonPost.build.millSourcePath,
    $file.example.httpMethods.build.millSourcePath,
    $file.example.minimalApplication.build.millSourcePath,
    $file.example.minimalApplication2.build.millSourcePath,
    $file.example.redirectAbort.build.millSourcePath,
    $file.example.scalatags.build.millSourcePath,
    $file.example.staticFiles.build.millSourcePath,
    $file.example.staticFiles2.build.millSourcePath,
    $file.example.todo.build.millSourcePath,
    $file.example.todoApi.build.millSourcePath,
    $file.example.todoDb.build.millSourcePath,
    $file.example.twirl.build.millSourcePath,
    $file.example.variableRoutes.build.millSourcePath,
    $file.example.websockets.build.millSourcePath,
    $file.example.websockets2.build.millSourcePath,
    $file.example.websockets3.build.millSourcePath,
    $file.example.websockets4.build.millSourcePath,
  )
  for(example <- examples){
    val f = T.ctx().dest
    val last = example.last + "-" + label
    os.copy(example, f / last)
    os.write.over(
      f / last / "mill",
      os.read(os.pwd / "mill")
    )
    os.proc("chmod", "+x", f/last/"mill").call(f/last)
    os.write.over(
      f / last / "build.sc",
      os.read(f / last / "build.sc")
        .replace("trait AppModule ", "object app ")
        .replaceFirst(
          "def ivyDeps = Agg\\[Dep\\]\\(",
          "def ivyDeps = Agg(\n    ivy\"com.lihaoyi::cask:" + releaseTag + "\""
        )
    )

    os.remove.all(f / "out.zip")
    os.proc("zip", "-r", f / "out.zip", last).call(f)
    upload.apply(f / "out.zip", releaseTag, last + ".zip", authKey)
  }
}

