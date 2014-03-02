import sbt._, Keys._
import sbtrelease._
import xerial.sbt.Sonatype._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys

object build extends Build {

  def gitHash: Option[String] = scala.util.Try(
    sys.process.Process("git show -s --oneline").lines_!.head.split(" ").head
  ).toOption

  val showDoc = TaskKey[Unit]("showDoc")

  val sonatypeURL = "https://oss.sonatype.org/service/local/repositories/"

  val updateReadme = { state: State =>
    val extracted = Project.extract(state)
    val scalaV = extracted get scalaBinaryVersion
    val v = extracted get version
    val org =  extracted get organization
    val n = "ghscala-core"
    val snapshotOrRelease = if(extracted get isSnapshot) "snapshots" else "releases"
    val readme = "README.md"
    val readmeFile = file(readme)
    val newReadme = Predef.augmentString(IO.read(readmeFile)).lines.map{ line =>
      val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
      if(line.startsWith("libraryDependencies") && matchReleaseOrSnapshot){
        s"""libraryDependencies += "${org}" %% "${n}" % "$v""""
      }else if(line.contains(sonatypeURL) && matchReleaseOrSnapshot){
        s"- [API Documentation](${sonatypeURL}${snapshotOrRelease}/archive/${org.replace('.','/')}/${n}_${scalaV}/${v}/${n}_${scalaV}-${v}-javadoc.jar/!/index.html)"
      }else line
    }.mkString("", "\n", "\n")
    IO.write(readmeFile, newReadme)
    val git = new Git(extracted get baseDirectory)
    git.add(readme) ! state.log
    git.commit("update " + readme) ! state.log
    "git diff HEAD^" ! state.log
    state
  }

  val updateReadmeProcess: ReleaseStep = updateReadme

  val baseSettings = ReleasePlugin.releaseSettings ++ sonatypeSettings ++ Seq(
    commands += Command.command("updateReadme")(updateReadme),
    ReleasePlugin.ReleaseKeys.releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      updateReadmeProcess,
      tagRelease,
      ReleaseStep(
        action = { state =>
          val extracted = Project extract state
          extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
        },
        enableCrossBuild = true
      ),
      setNextVersion,
      commitNextVersion,
      ReleaseStep{ state =>
        val extracted = Project extract state
        extracted.runAggregated(SonatypeKeys.sonatypeReleaseAll in Global in extracted.get(thisProjectRef), state)
      },
      updateReadmeProcess,
      pushChanges
    ),
    credentials ++= PartialFunction.condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASS")){
      case (Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
    }.toList,
    organization := "com.github.xuwei-k",
    description := "purely functional scala github api client",
    homepage := Some(url("https://github.com/xuwei-k/ghscala")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/xuwei-k/ghscala"),
      "scm:git:git@github.com/xuwei-k/ghscala.git"
    )),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-language:_"),
    scalaVersion := "2.10.3",
    scalacOptions in (Compile, doc) ++= {
      val tag = if(isSnapshot.value) gitHash.getOrElse("master") else { "v" + version.value }
      Seq(
        "-sourcepath", baseDirectory.value.getAbsolutePath,
        "-doc-source-url", s"https://github.com/xuwei-k/ghscala/tree/${tag}€{FILE_PATH}.scala"
      )
    },
    logBuffered in Test := false,
    pomExtra := (
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
      </developers>
    ),
    showDoc in Compile <<= (doc in Compile, target in doc in Compile) map { (_, out) =>
      java.awt.Desktop.getDesktop.open(out / "index.html")
    }
  )

  lazy val core = Project("core", file("core")).settings(
    baseSettings : _*
  ).settings(
    name := "ghscala-core",
    libraryDependencies ++= Seq(
      "io.argonaut" %% "argonaut" % "6.1-M2",
      "org.scalaz" %% "scalaz-concurrent" % "7.1.0-M3",
      "joda-time" % "joda-time" % "2.3",
      "org.joda" % "joda-convert" % "1.2",
      "commons-codec" % "commons-codec" % "1.9"
    )
  )

  lazy val scalaj = Project("scalaj", file("scalaj")).settings(
    baseSettings : _*
  ).settings(
    name := "ghscala-scalaj",
    libraryDependencies ++= Seq(
      "org.scalaj"  %% "scalaj-http" % "0.3.14"
    )
  ).dependsOn(core)

  lazy val dispatch = Project("dispatch", file("dispatch")).settings(
    baseSettings : _*
  ).settings(
    name := "ghscala-dispatch",
    libraryDependencies ++= Seq(
      "net.databinder" %% "dispatch-http" % "0.8.10"
    )
  ).dependsOn(core)

  lazy val root = Project("root", file(".")).settings(
    baseSettings ++ Seq(
      publishArtifact := false,
      publish := {},
      publishLocal := {}
    ): _*
  ).aggregate(core, scalaj, dispatch)


}

