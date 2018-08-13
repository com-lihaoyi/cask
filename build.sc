import mill._, scalalib._, publish._
import ammonite.ops._, ujson.Js
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
import $file.example.todo.build
import $file.example.todoApi.build
import $file.example.todoDb.build
import $file.example.variableRoutes.build
import $file.example.websockets.build

object cask extends ScalaModule with PublishModule {
  def scalaVersion = "2.12.6"
  def ivyDeps = Agg(
    ivy"org.scala-lang:scala-reflect:${scalaVersion()}",
    ivy"io.undertow:undertow-core:2.0.11.Final",
    ivy"com.lihaoyi::upickle:0.6.6",
  )
  def compileIvyDeps = Agg(ivy"com.lihaoyi::acyclic:0.1.7")
  def scalacOptions = Seq("-P:acyclic:force")
  def scalacPluginIvyDeps = Agg(ivy"com.lihaoyi::acyclic:0.1.7")

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

  object test extends Tests{

    def testFrameworks = Seq("utest.runner.Framework")
    def ivyDeps = Agg(
      ivy"com.lihaoyi::utest::0.6.3",
      ivy"com.lihaoyi::requests::0.1.2",
      ivy"org.xerial:sqlite-jdbc:3.18.0",
      ivy"io.getquill::quill-jdbc:2.5.4"
    )
  }
}
object example extends Module{
  trait LocalModule extends ScalaModule{
    def ivyDeps = super.ivyDeps().filter(_ != ivy"com.lihaoyi::cask:0.0.6")

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
  object todo extends $file.example.todo.build.AppModule with LocalModule
  object todoApi extends $file.example.todoApi.build.AppModule with LocalModule
  object todoDb extends $file.example.todoDb.build.AppModule with LocalModule
  object variableRoutes extends $file.example.variableRoutes.build.AppModule with LocalModule
  object websockets extends $file.example.websockets.build.AppModule with LocalModule
}

def publishVersion = T.input($file.ci.version.publishVersion)
def gitHead = T.input($file.ci.version.gitHead)

def uploadToGithub(authKey: String) = T.command{
  val (releaseTag, label) = publishVersion()

  if (releaseTag == label){
    scalaj.http.Http("https://api.github.com/repos/lihaoyi/cask/releases")
      .postData(
        ujson.write(
          Js.Obj(
            "tag_name" -> releaseTag,
            "name" -> releaseTag
          )
        )
      )
      .header("Authorization", "token " + authKey)
      .asString
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
    $file.example.todo.build.millSourcePath,
    $file.example.todoApi.build.millSourcePath,
    $file.example.todoDb.build.millSourcePath,
    $file.example.variableRoutes.build.millSourcePath,
    $file.example.websockets.build.millSourcePath,
  )
  for(example <- examples){
    val f = tmp.dir()
    cp(example, f/'folder)
    write.over(
      f/'folder/"cask",
      """#!/usr/bin/env bash
        |
        |if [ ! -f out/mill-cask ]; then
        |  echo "Initializing Cask/Mill build tool for the first time"
        |  mkdir -p out &&
        |  (echo "#!/usr/bin/env sh" && curl -L https://github.com/lihaoyi/mill/releases/download/0.2.6/0.2.6) > out/mill-cask
        |fi
        |
        |chmod +x out/mill-cask
        |"$(pwd)"/out/mill-cask "$@"
      """.stripMargin
    )
    %%("chmod", "+x", f/'folder/"cask")(f/'folder)
    write.over(
      f/'folder/"build.sc",
      read(f/'folder/"build.sc").replace("trait AppModule ", "object app ")
    )

    %%("zip", "-r", f/"out.zip", f/'folder)(f/'folder)
    upload.apply(f/"out.zip", releaseTag, label + "/" + example.last + ".zip", authKey)
  }
}

