val isCommunityBuild =
  sys.props.getOrElse("community", "true").toBoolean

lazy val baseModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-sql`
)

lazy val dbModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-jdbc`
)

lazy val jasyncModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-jasync`, `quill-jasync-postgres`
)

lazy val allModules =
  baseModules ++ dbModules ++ jasyncModules

val filteredModules = {
  allModules
}

lazy val `quill` = {
  val quill =
    (project in file("."))
      .settings(commonSettings: _*)
      .aggregate(filteredModules.map(_.project): _*)
      .dependsOn(filteredModules: _*)
}


lazy val `scalatest-shim` =
  (project in file("scalatest-shim"))
    .settings(basicSettings: _*)

lazy val `quill-sql` =
  (project in file("quill-sql"))
    .settings(commonSettings: _*)
    .settings(
      resolvers ++= Seq(
        Resolver.mavenLocal,
        "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
        "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
      ),
      libraryDependencies ++= Seq(
        // .excludeAll(ExclusionRule(organization="com.trueaccord.scalapb")
        ("com.lihaoyi" %% "pprint" % "0.5.6").withDottyCompat(scalaVersion.value),
        ("io.getquill" %% "quill-core-portable" % "3.7.1").withDottyCompat(scalaVersion.value),
        ("io.getquill" %% "quill-sql-portable" % "3.7.1").withDottyCompat(scalaVersion.value),
        //("org.scalameta" %% "scalafmt-dynamic" % "2.7.4").withDottyCompat(scalaVersion.value),
        //"org.scala-lang" % "scala3-library_3.0.0-M3" % (scalaVersion.value),
      ),
      // If it's a community-build we're using a scala incremental version so there's no scalatest for that
      libraryDependencies ++= {
        if (isCommunityBuild)
          Seq()
        else
          Seq(
            "org.scalatest" % "scalatest_3" % "3.2.9" % "test",
            "org.scalatest" % "scalatest-mustmatchers_3" % "3.2.9" % "test"
          )
      },
      // If it's a community-build we're using a scala incremental and scalafmt doesn't seem to work well with that
      libraryDependencies ++= {
        if (isCommunityBuild)
          Seq()
        else
          Seq(("org.scalameta" %% "scalafmt-cli" % "2.7.5" ).excludeAll(ExclusionRule(organization = "org.scala-lang.modules", name = "scala-xml_2.13")).withDottyCompat(scalaVersion.value))
      }
    ).dependsOn({
      // If it's a community build, we cannot include scalatest since the scalatest for the corresponding
      // incremental scala version does not exist. So we need to include this module that "shims-it-out" so we can just be able
      // to compile stuff (i.e. on an incremental scala version)
      if (isCommunityBuild)
        Seq(`scalatest-shim` % "test->test")
      else
        Seq()
    }: _*)

// Moving heavy tests to separate module so it can be compiled in parallel with others
lazy val `quill-sql-tests` =
  (project in file("quill-sql-tests"))
    .settings(commonSettings: _*)
    .dependsOn(`quill-sql` % "compile->compile;test->test")

//lazy val `quill-sql-all` = (project in file(".")).aggregate(`quill-sql`, `quill-sql-tests`)

lazy val `quill-jdbc` =
  (project in file("quill-jdbc"))
    .settings(commonSettings: _*)
    .settings(jdbcTestingSettings: _*)
    .dependsOn(`quill-sql` % "compile->compile;test->test")

lazy val `quill-jasync` =
  (project in file("quill-jasync"))
    .settings(commonSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-common" % "1.1.4",
        ("org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1").withDottyCompat(scalaVersion.value)
      )
    )
    .dependsOn(`quill-sql` % "compile->compile;test->test")

lazy val `quill-jasync-postgres` =
  (project in file("quill-jasync-postgres"))
    .settings(commonSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-postgresql" % "1.1.4"
      )
    )
    .dependsOn(`quill-jasync` % "compile->compile;test->test")

// Include scalafmt formatter for pretty printing failed queries
val includeFormatter =
  sys.props.getOrElse("formatScala", "false").toBoolean

lazy val commonSettings = /* ReleasePlugin.extraReleaseCommands ++  */ basicSettings

lazy val jdbcTestingLibraries = Seq(
  libraryDependencies ++= Seq(
    "com.zaxxer"              %  "HikariCP"                % "3.4.5",
    "mysql"                   %  "mysql-connector-java"    % "8.0.22"             % Test,
    "com.h2database"          %  "h2"                      % "1.4.200"            % Test,
    "org.postgresql"          %  "postgresql"              % "42.2.18"             % Test,
    "org.xerial"              %  "sqlite-jdbc"             % "3.32.3.2"             % Test,
    "com.microsoft.sqlserver" %  "mssql-jdbc"              % "7.1.1.jre8-preview" % Test,
    "com.oracle.ojdbc"        %  "ojdbc8"                  % "19.3.0.0"           % Test,
    //"org.mockito"             %% "mockito-scala-scalatest" % "1.16.2"              % Test
  )
)

lazy val jdbcTestingSettings = jdbcTestingLibraries ++ Seq(
  fork in Test := true
)

lazy val basicSettings = Seq(
  scalaVersion := {
    if (isCommunityBuild) dottyLatestNightlyBuild.get else "3.0.0"
  },
  scalacOptions ++= Seq(
    "-language:implicitConversions"
  )
)
