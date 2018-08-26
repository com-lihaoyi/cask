package test.cask

import utest._

object UtilTests extends TestSuite {
  val tests = Tests{
    'splitPath - {
      cask.internal.Util.splitPath("") ==> Seq()
      cask.internal.Util.splitPath("/") ==> Seq()
      cask.internal.Util.splitPath("////") ==> Seq()

      cask.internal.Util.splitPath("abc") ==> Seq("abc")
      cask.internal.Util.splitPath("/abc/") ==> Seq("abc")
      cask.internal.Util.splitPath("//abc") ==> Seq("abc")
      cask.internal.Util.splitPath("abc//") ==> Seq("abc")

      cask.internal.Util.splitPath("abc//def") ==> Seq("abc", "def")
      cask.internal.Util.splitPath("//abc//def//") ==> Seq("abc", "def")
    }
  }
}

