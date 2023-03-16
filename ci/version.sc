val isMasterCommit = {
  sys.env.get("TRAVIS_PULL_REQUEST").contains("false") &&
    (sys.env.get("TRAVIS_BRANCH").contains("master") || sys.env("TRAVIS_TAG") != "")
}

def gitHead =
  sys.env.get("TRAVIS_COMMIT").getOrElse(
    os.proc("git", "rev-parse", "HEAD").call().out.string.trim()
  )



def publishVersion = {
  val tag =
    try Option(
      os.proc("git", "describe", "--exact-match", "--tags", "--always", gitHead).call().out.string.trim()
    )
    catch{case e => None}

  val dirtySuffix = os.proc("git", "diff").call().out.string.trim() match{
    case "" => ""
    case s => "-DIRTY" + Integer.toHexString(s.hashCode)
  }

  tag match{
    case Some(t) => (t, t)
    case None =>
      val latestTaggedVersion = os.proc("git", "describe", "--abbrev=0", "--always", "--tags").call().out.trim

      val commitsSinceLastTag =
        os.proc("git", "rev-list", gitHead, "--not", latestTaggedVersion, "--count").call().out.trim.toInt

      (latestTaggedVersion, s"$latestTaggedVersion-$commitsSinceLastTag-${gitHead.take(6)}$dirtySuffix")
  }
}
