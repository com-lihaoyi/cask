import ammonite.ops.{%%, pwd}

val isMasterCommit = {
  sys.env.get("TRAVIS_PULL_REQUEST").contains("false") &&
    (sys.env.get("TRAVIS_BRANCH").contains("master") || sys.env("TRAVIS_TAG") != "")
}

def gitHead =
  sys.env.get("TRAVIS_COMMIT").getOrElse(
    %%('git, "rev-parse", "HEAD")(pwd).out.string.trim()
  )



def publishVersion = {
  val tag =
    try Option(
      %%('git, 'describe, "--exact-match", "--tags", "--always", gitHead)(pwd).out.string.trim()
    )
    catch{case e => None}

  val dirtySuffix = %%('git, 'diff)(pwd).out.string.trim() match{
    case "" => ""
    case s => "-DIRTY" + Integer.toHexString(s.hashCode)
  }

  tag match{
    case Some(t) => (t, t)
    case None =>
      val latestTaggedVersion = %%('git, 'describe, "--abbrev=0", "--always", "--tags")(pwd).out.trim

      val commitsSinceLastTag =
        %%('git, "rev-list", gitHead, "--not", latestTaggedVersion, "--count")(pwd).out.trim.toInt

      (latestTaggedVersion, s"$latestTaggedVersion-$commitsSinceLastTag-${gitHead.take(6)}$dirtySuffix")
  }
}
