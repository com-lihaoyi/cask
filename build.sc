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

val scala213  = "2.13.2"
val scala3 = "0.26.0-RC1"
val dottyCustomVersion = Option(sys.props("dottyVersion"))

trait CaskModule extends CrossScalaModule with PublishModule{
  def isDotty = crossScalaVersion.startsWith("0")

  // // sources can't be a Target (mill actually requires a Sources), so we need to
  // // statically check for the version
  // // TODO: fix mill to allow specifying a Target?
  // def sources = if (dottyVersion.isDefined) {
  //   T.sources(
  //     millSourcePath / "src",
  //     millSourcePath / "src-0"
  //   )
  // } else {
  //   T.sources(
  //     millSourcePath / "src",
  //     millSourcePath / "src-2"
  //   )
  // }

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

class CaskMainModule(val crossScalaVersion: String) extends CaskModule {
  def ivyDeps = T{
    Agg(
      ivy"io.undertow:undertow-core:2.0.13.Final",
      ivy"com.lihaoyi::upickle:1.1.0"
    ) ++
    (if(!isDotty) Agg(ivy"org.scala-lang:scala-reflect:${scalaVersion()}") else Agg())
  }
  def compileIvyDeps = T{ if (!isDotty) Agg(ivy"com.lihaoyi::acyclic:0.2.0") else Agg() }
  def scalacOptions = T{ if (!isDotty) Seq("-P:acyclic:force") else Seq() }
  def scalacPluginIvyDeps = T{ if (!isDotty) Agg(ivy"com.lihaoyi::acyclic:0.2.0") else Agg() }

  object test extends Tests{
    def testFrameworks = Seq("utest.runner.Framework")
    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.7.4",
      ivy"com.lihaoyi::requests::0.6.5"
    )
  }
  def moduleDeps = Seq(util.jvm(crossScalaVersion))
}
object cask extends Cross[CaskMainModule]((Seq(scala213, scala3) ++ dottyCustomVersion): _*)

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
      ivy"com.lihaoyi::sourcecode:0.2.1",
      //ivy"com.lihaoyi::pprint:0.5.9",
      ivy"com.lihaoyi::geny:0.6.2"
    )
  }
  class UtilJvmModule(val crossScalaVersion: String) extends UtilModule {
    def platformSegment = "jvm"
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"com.lihaoyi::castor::0.1.7".withDottyCompat(scalaVersion()),
      ivy"org.java-websocket:Java-WebSocket:1.4.0"
    )
  }
  object jvm extends Cross[UtilJvmModule]((Seq(scala213, scala3) ++ dottyCustomVersion): _*)

  class UtilJsModule(val crossScalaVersion: String) extends UtilModule with ScalaJSModule {
    def platformSegment = "js"
    def scalaJSVersion = "0.6.33"
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"com.lihaoyi::castor::0.1.7",
      ivy"org.scala-js::scalajs-dom::0.9.7"
    )
  }
  object js extends Cross[UtilJsModule](scala213)

}

object example extends Module{
  trait LocalModule extends CrossScalaModule{
    override def millSourcePath = super.millSourcePath / "app"
    def moduleDeps = Seq(cask(crossScalaVersion))
  }

  val allVersions = Seq(scala213, scala3) ++ dottyCustomVersion

  //object (\w+) (extends \$file.*)
  //class $1Module(val crossScalaVersion: String) $2\n object $1 extends Cross[$1Module](allVersions: _*)\n

  class compressModule(val crossScalaVersion: String) extends $file.example.compress.build.AppModule with LocalModule
  object compress extends Cross[compressModule](allVersions: _*)

  class compress2Module(val crossScalaVersion: String) extends $file.example.compress2.build.AppModule with LocalModule
  object compress2 extends Cross[compress2Module](allVersions: _*)

  class compress3Module(val crossScalaVersion: String) extends $file.example.compress3.build.AppModule with LocalModule
  object compress3 extends Cross[compress3Module](allVersions: _*)

  class cookiesModule(val crossScalaVersion: String) extends $file.example.cookies.build.AppModule with LocalModule
  object cookies extends Cross[cookiesModule](allVersions: _*)

  class decoratedModule(val crossScalaVersion: String) extends $file.example.decorated.build.AppModule with LocalModule
  object decorated extends Cross[decoratedModule](allVersions: _*)

  class decorated2Module(val crossScalaVersion: String) extends $file.example.decorated2.build.AppModule with LocalModule
  object decorated2 extends Cross[decorated2Module](allVersions: _*)

  class endpointsModule(val crossScalaVersion: String) extends $file.example.endpoints.build.AppModule with LocalModule
  object endpoints extends Cross[endpointsModule](allVersions: _*)

  class formJsonPostModule(val crossScalaVersion: String) extends $file.example.formJsonPost.build.AppModule with LocalModule
  object formJsonPost extends Cross[formJsonPostModule](allVersions: _*)

  class httpMethodsModule(val crossScalaVersion: String) extends $file.example.httpMethods.build.AppModule with LocalModule
  object httpMethods extends Cross[httpMethodsModule](allVersions: _*)

  class MinimalApplicationModule(val crossScalaVersion: String) extends $file.example.minimalApplication.build.AppModule with LocalModule
  object minimalApplication extends Cross[MinimalApplicationModule](allVersions: _*)

  class minimalApplication2Module(val crossScalaVersion: String) extends $file.example.minimalApplication2.build.AppModule with LocalModule
  object minimalApplication2 extends Cross[minimalApplication2Module](allVersions: _*)

  class redirectAbortModule(val crossScalaVersion: String) extends $file.example.redirectAbort.build.AppModule with LocalModule
  object redirectAbort extends Cross[redirectAbortModule](allVersions: _*)

  class scalatagsModule(val crossScalaVersion: String) extends $file.example.scalatags.build.AppModule with LocalModule
  object scalatags extends Cross[scalatagsModule](scala213) // TODO: enable Dotty once scalatags has been ported

  class staticFilesModule(val crossScalaVersion: String) extends $file.example.staticFiles.build.AppModule with LocalModule
  object staticFiles extends Cross[staticFilesModule](allVersions: _*)

  class staticFiles2Module(val crossScalaVersion: String) extends $file.example.staticFiles2.build.AppModule with LocalModule
  object staticFiles2 extends Cross[staticFiles2Module](allVersions: _*)

  class todoModule(val crossScalaVersion: String) extends $file.example.todo.build.AppModule with LocalModule
  object todo extends Cross[todoModule](scala213) // TODO: uses quill, can't enable for Dotty yet

  class todoApiModule(val crossScalaVersion: String) extends $file.example.todoApi.build.AppModule with LocalModule
  object todoApi extends Cross[todoApiModule](scala213) // TODO: enable

  class todoDbModule(val crossScalaVersion: String) extends $file.example.todoDb.build.AppModule with LocalModule
  object todoDb extends Cross[todoDbModule](scala213) // TODO: uses quill, can't enable for Dotty yet

  class twirlModule(val crossScalaVersion: String) extends $file.example.twirl.build.AppModule with LocalModule
  object twirl extends Cross[twirlModule](scala213) // TODO: enable once twirl is available

  class variableRoutesModule(val crossScalaVersion: String) extends $file.example.variableRoutes.build.AppModule with LocalModule
  object variableRoutes extends Cross[variableRoutesModule](allVersions: _*)

  class websocketsModule(val crossScalaVersion: String) extends $file.example.websockets.build.AppModule with LocalModule
  object websockets extends Cross[websocketsModule](allVersions: _*)

  class websockets2Module(val crossScalaVersion: String) extends $file.example.websockets2.build.AppModule with LocalModule
  object websockets2 extends Cross[websockets2Module](allVersions: _*)

  class websockets3Module(val crossScalaVersion: String) extends $file.example.websockets3.build.AppModule with LocalModule
  object websockets3 extends Cross[websockets3Module](allVersions: _*)

  class websockets4Module(val crossScalaVersion: String) extends $file.example.websockets4.build.AppModule with LocalModule
  object websockets4 extends Cross[websockets4Module](allVersions: _*)

}

def publishVersion = T.input($file.ci.version.publishVersion)
def gitHead = T.input($file.ci.version.gitHead)

// def uploadToGithub(authKey: String) = T.command{
//   val (releaseTag, label) = publishVersion()

//   if (releaseTag == label){
//     requests.post(
//       "https://api.github.com/repos/lihaoyi/cask/releases",
//       data = ujson.write(
//         ujson.Obj(
//           "tag_name" -> releaseTag,
//           "name" -> releaseTag
//         )
//       ),
//       headers = Seq("Authorization" -> s"token $authKey")
//     )
//   }

//   val examples = Seq(
//     // $file.example.compress.build.millSourcePath,
//     // $file.example.compress2.build.millSourcePath,
//     // $file.example.compress3.build.millSourcePath,
//     // $file.example.cookies.build.millSourcePath,
//     // $file.example.decorated.build.millSourcePath,
//     // $file.example.decorated2.build.millSourcePath,
//     // $file.example.endpoints.build.millSourcePath,
//     // $file.example.formJsonPost.build.millSourcePath,
//     // $file.example.httpMethods.build.millSourcePath,
//     $file.example.minimalApplication.build.millSourcePath,
//     // $file.example.minimalApplication2.build.millSourcePath,
//     // $file.example.redirectAbort.build.millSourcePath,
//     // $file.example.scalatags.build.millSourcePath,
//     // $file.example.staticFiles.build.millSourcePath,
//     // $file.example.staticFiles2.build.millSourcePath,
//     // $file.example.todo.build.millSourcePath,
//     // $file.example.todoApi.build.millSourcePath,
//     // $file.example.todoDb.build.millSourcePath,
//     // $file.example.twirl.build.millSourcePath,
//     // $file.example.variableRoutes.build.millSourcePath,
//     // $file.example.websockets.build.millSourcePath,
//     // $file.example.websockets2.build.millSourcePath,
//     // $file.example.websockets3.build.millSourcePath,
//     // $file.example.websockets4.build.millSourcePath,
//   )
//   for(example <- examples){
//     val f = T.ctx().dest
//     val last = example.last + "-" + label
//     os.copy(example, f / last)
//     os.write.over(
//       f / last / "mill",
//       os.read(os.pwd / "mill")
//     )
//     os.proc("chmod", "+x", f/last/"mill").call(f/last)
//     os.write.over(
//       f / last / "build.sc",
//       os.read(f / last / "build.sc")
//         .replace("trait AppModule ", "object app ")
//         .replaceFirst(
//           "def ivyDeps = Agg\\[Dep\\]\\(",
//           "def ivyDeps = Agg(\n    ivy\"com.lihaoyi::cask:" + releaseTag + "\","
//         )
//     )

//     os.remove.all(f / "out.zip")
//     os.proc("zip", "-r", f / "out.zip", last).call(f)
//     upload.apply(f / "out.zip", releaseTag, last + ".zip", authKey)
//   }
// }

