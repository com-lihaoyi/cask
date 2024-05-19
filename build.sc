import mill._, scalalib._, scalajslib._, publish._
import mill.scalalib.api.ZincWorkerUtil

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
import $file.example.queryParams.build
import $file.example.websockets.build
import $file.example.websockets2.build
import $file.example.websockets3.build
import $file.example.websockets4.build
import $file.ci.upload
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.github.lolgab::mill-mima::0.1.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion

val scala213 = "2.13.14"
val scala212 = "2.12.19"
val scala3 = "3.3.3"
val scalaJS = "1.16.0"
val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scalaVersions = List(scala212, scala213, scala3) ++ communityBuildDottyVersion

trait CaskModule extends CrossScalaModule with PublishModule{
  def isScala3 = ZincWorkerUtil.isScala3(crossScalaVersion)

  def publishVersion = VcsVersion.vcsState().format()

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/cask",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("com-lihaoyi", "cask"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
    )
  )
}

trait CaskMainModule extends CaskModule {
  def ivyDeps = T{
    Agg(
      ivy"io.undertow:undertow-core:2.3.10.Final",
      ivy"com.lihaoyi::upickle:3.0.0"
    ) ++
    Agg.when(!isScala3)(ivy"org.scala-lang:scala-reflect:$crossScalaVersion")
  }

  def compileIvyDeps = Agg.when(!isScala3)(ivy"com.lihaoyi:::acyclic:0.3.12")
  def scalacOptions = Agg.when(!isScala3)("-P:acyclic:force").toSeq
  def scalacPluginIvyDeps = Agg.when(!isScala3)(ivy"com.lihaoyi:::acyclic:0.3.12")

  object test extends ScalaTests with TestModule.Utest{
    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.8.1",
      ivy"com.lihaoyi::requests::0.8.0"
    )
  }
  def moduleDeps = Seq(cask.util.jvm(crossScalaVersion))
}

object cask extends Cross[CaskMainModule](scalaVersions) {
  object util extends Module {
    trait UtilModule extends CaskModule with PlatformScalaModule{
      def ivyDeps = Agg(
        ivy"com.lihaoyi::sourcecode:0.3.0",
        ivy"com.lihaoyi::pprint:0.8.1",
        ivy"com.lihaoyi::geny:1.0.0"
      )
    }

    object jvm extends Cross[UtilJvmModule](scalaVersions)
    trait UtilJvmModule extends UtilModule {
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::castor::0.3.0",
        ivy"org.java-websocket:Java-WebSocket:1.5.3"
      )
    }

    object js extends Cross[UtilJsModule](scala213)
    trait UtilJsModule extends UtilModule with ScalaJSModule {
      def scalaJSVersion = scalaJS
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::castor::0.3.0",
        ivy"org.scala-js::scalajs-dom::2.4.0"
      )
    }
  }
}

object example extends Module{
  trait LocalModule extends CrossScalaModule{
    override def millSourcePath = super.millSourcePath / "app"
    def moduleDeps = Seq(cask(crossScalaVersion))
  }

  trait CompressModule extends millbuild.example.compress.build.AppModule with LocalModule
  object compress extends Cross[CompressModule](scalaVersions)

  trait Compress2Module extends millbuild.example.compress2.build.AppModule with LocalModule
  object compress2 extends Cross[Compress2Module](scalaVersions)

  trait Compress3Module extends millbuild.example.compress3.build.AppModule with LocalModule
  object compress3 extends Cross[Compress3Module](scalaVersions)

  trait CookiesModule extends millbuild.example.cookies.build.AppModule with LocalModule
  object cookies extends Cross[CookiesModule](scalaVersions)

  trait DecoratedModule extends millbuild.example.decorated.build.AppModule with LocalModule
  object decorated extends Cross[DecoratedModule](scalaVersions)

  trait Decorated2Module extends millbuild.example.decorated2.build.AppModule with LocalModule
  object decorated2 extends Cross[Decorated2Module](scalaVersions)

  trait EndpointsModule extends millbuild.example.endpoints.build.AppModule with LocalModule
  object endpoints extends Cross[EndpointsModule](scalaVersions)

  trait FormJsonPostModule extends millbuild.example.formJsonPost.build.AppModule with LocalModule
  object formJsonPost extends Cross[FormJsonPostModule](scalaVersions)

  trait HttpMethodsModule extends millbuild.example.httpMethods.build.AppModule with LocalModule
  object httpMethods extends Cross[HttpMethodsModule](scalaVersions)

  trait MinimalApplicationModule extends millbuild.example.minimalApplication.build.AppModule with LocalModule
  object minimalApplication extends Cross[MinimalApplicationModule](scalaVersions)

  trait MinimalApplication2Module extends millbuild.example.minimalApplication2.build.AppModule with LocalModule
  object minimalApplication2 extends Cross[MinimalApplication2Module](scalaVersions)

  trait RedirectAbortModule extends millbuild.example.redirectAbort.build.AppModule with LocalModule
  object redirectAbort extends Cross[RedirectAbortModule](scalaVersions)

  trait ScalatagsModule extends millbuild.example.scalatags.build.AppModule with LocalModule
  object scalatags extends Cross[ScalatagsModule](scala212, scala213)

  trait StaticFilesModule extends millbuild.example.staticFiles.build.AppModule with LocalModule
  object staticFiles extends Cross[StaticFilesModule](scalaVersions)

  trait StaticFiles2Module extends millbuild.example.staticFiles2.build.AppModule with LocalModule
  object staticFiles2 extends Cross[StaticFiles2Module](scalaVersions)

  trait TodoModule extends millbuild.example.todo.build.AppModule with LocalModule
  object todo extends Cross[TodoModule](scala213) // uses quill, can't enable for Dotty yet

  trait TodoApiModule extends millbuild.example.todoApi.build.AppModule with LocalModule
  object todoApi extends Cross[TodoApiModule](scalaVersions)

  trait TodoDbModule extends millbuild.example.todoDb.build.AppModule with LocalModule
  object todoDb extends Cross[TodoDbModule](scala213) // uses quill, can't enable for Dotty yet

  trait TwirlModule extends millbuild.example.twirl.build.AppModule with LocalModule
  object twirl extends Cross[TwirlModule](scalaVersions)

  trait VariableRoutesModule extends millbuild.example.variableRoutes.build.AppModule with LocalModule
  object variableRoutes extends Cross[VariableRoutesModule](scalaVersions)

  trait QueryParamsModule extends millbuild.example.variableRoutes.build.AppModule with LocalModule
  object queryParams extends Cross[QueryParamsModule](scalaVersions)

  trait WebsocketsModule extends millbuild.example.websockets.build.AppModule with LocalModule
  object websockets extends Cross[WebsocketsModule](scalaVersions)

  trait Websockets2Module extends millbuild.example.websockets2.build.AppModule with LocalModule
  object websockets2 extends Cross[Websockets2Module](scalaVersions)

  trait Websockets3Module extends millbuild.example.websockets3.build.AppModule with LocalModule
  object websockets3 extends Cross[Websockets3Module](scalaVersions)

  trait Websockets4Module extends millbuild.example.websockets4.build.AppModule with LocalModule
  object websockets4 extends Cross[Websockets4Module](scalaVersions)

}



def uploadToGithub() = T.command{
  val vcsState = VcsVersion.vcsState()

  val authKey = T.env.apply("AMMONITE_BOT_AUTH_TOKEN")
  val releaseTag = vcsState.lastTag.getOrElse("")
  val label = vcsState.format()
  if (releaseTag == label){
    requests.post(
      "https://api.github.com/repos/com-lihaoyi/cask/releases",
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
    millbuild.example.compress.build.millSourcePath,
    millbuild.example.compress2.build.millSourcePath,
    millbuild.example.compress3.build.millSourcePath,
    millbuild.example.cookies.build.millSourcePath,
    millbuild.example.decorated.build.millSourcePath,
    millbuild.example.decorated2.build.millSourcePath,
    millbuild.example.endpoints.build.millSourcePath,
    millbuild.example.formJsonPost.build.millSourcePath,
    millbuild.example.httpMethods.build.millSourcePath,
    millbuild.example.minimalApplication.build.millSourcePath,
    millbuild.example.minimalApplication2.build.millSourcePath,
    millbuild.example.redirectAbort.build.millSourcePath,
    millbuild.example.scalatags.build.millSourcePath,
    millbuild.example.staticFiles.build.millSourcePath,
    millbuild.example.staticFiles2.build.millSourcePath,
    millbuild.example.todo.build.millSourcePath,
    millbuild.example.todoApi.build.millSourcePath,
    millbuild.example.todoDb.build.millSourcePath,
    millbuild.example.twirl.build.millSourcePath,
    millbuild.example.variableRoutes.build.millSourcePath,
    millbuild.example.queryParams.build.millSourcePath,
    millbuild.example.websockets.build.millSourcePath,
    millbuild.example.websockets2.build.millSourcePath,
    millbuild.example.websockets3.build.millSourcePath,
    millbuild.example.websockets4.build.millSourcePath,
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
        .replaceFirst(
          "trait AppModule extends CrossScalaModule\\s*\\{",
          s"object app extends ScalaModule \\{\n  def scalaVersion = \"${scala213}\"")
        .replaceFirst(
          "def ivyDeps = Agg\\[Dep\\]\\(",
          "def ivyDeps = Agg(\n    ivy\"com.lihaoyi::cask:" + releaseTag + "\","
        )
    )

    os.remove.all(f / "out.zip")
    os.proc("zip", "-r", f / "out.zip", last).call(f)
    upload.apply(f / "out.zip", releaseTag, last + ".zip", authKey)
  }
}
