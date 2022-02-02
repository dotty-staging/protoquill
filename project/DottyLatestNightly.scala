// Retrieve the version string of the most recent Dotty nightly build
// As originally implemented in the lampepfl sbt-dotty plugin

object DottyLatestNightly {
    // NOTE:
    // - this is a def to support `scalaVersion := dottyLatestNightlyBuild`
    // - if this was a taskKey, then you couldn't do `scalaVersion := dottyLatestNightlyBuild`
    // - if this was a settingKey, then this would evaluate even if you don't use it.
    def dottyLatestNightlyBuild(): Option[String] = {
      import scala.io.Source

      println("Fetching latest Dotty nightly version...")

      val nightly = try {
        // get majorVersion from dotty.epfl.ch
        val source0 = Source.fromURL("https://dotty.epfl.ch/versions/latest-nightly-base")
        val majorVersionFromWebsite = source0.getLines().toSeq.head
        source0.close()

        // get latest nightly version from maven
        def fetchSource(version: String): (scala.io.BufferedSource, String) =
          try {
            val url = s"https://repo1.maven.org/maven2/org/scala-lang/scala3-compiler_$version/maven-metadata.xml"
            Source.fromURL(url) -> version
          }
          catch { case t: java.io.FileNotFoundException =>
            val major :: minor :: Nil = version.split('.').toList
            if (minor.toInt <= 0) throw t
            else fetchSource(s"$major.${minor.toInt - 1}")
          }
        val (source1, majorVersion) = fetchSource(majorVersionFromWebsite)
        val Version = s"      <version>($majorVersion.*-bin.*)</version>".r
        val nightly = source1
          .getLines()
          .collect { case Version(version) => version }
          .toSeq
          .lastOption
        source1.close()
        nightly
      } catch {
        case _: java.net.UnknownHostException =>
          None
      }

      nightly match {
        case Some(version) =>
          println(s"Latest Dotty nightly build version: $version")
        case None =>
          println(s"Unable to get Dotty latest nightly build version. Make sure you are connected to internet")
      }

      nightly
    }
}
