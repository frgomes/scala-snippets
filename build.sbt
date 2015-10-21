import sbt._
import Keys._

lazy val buildSettings: Seq[Setting[_]] = 
  Defaults.defaultSettings ++ 
    librarySettings ++ runSettings

val javacOpts : Seq[String] =
  Seq(
    "-encoding", "UTF-8",
    "-source", "1.8",
    "-target", "1.8"
  )

val scalacOpts : Seq[String] =
  Seq(
    "-unchecked", "-deprecation", "-feature"
  )

val javacCheckOpts: Seq[String] =
  Seq(
    "-Xlint"
  )

val scalacCheckOpts: Seq[String] =
  Seq(
    "-Xlint",
    "-Yinline-warnings", "-Ywarn-adapted-args",
    "-Ywarn-dead-code", "-Ywarn-inaccessible", "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit", "-Xlog-free-terms"
  )

val librarySettings : Seq[Setting[_]] =
  Seq(
    organization       := "com.github.frgomes.scala",

    scalaVersion       := "2.11.7",
    crossScalaVersions := Seq("2.11.7"),
    crossPaths         := true,

    compileOrder in Compile := CompileOrder.JavaThenScala,
    compileOrder in Test    := CompileOrder.JavaThenScala,

    javacOptions  ++= javacOpts,
    scalacOptions ++= scalacOpts,

    libraryDependencies ++=
      Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4" ))

lazy val runSettings: Seq[Setting[_]] =
  Seq(
    fork in (Compile, run) := false)

def forkRunOptions(s: Scope): Seq[Setting[_]] =
  Seq(
    // see: https://github.com/sbt/sbt/issues/2247
    // see: https://github.com/sbt/sbt/issues/2244
    runner in run in s := {
      val forkOptions: ForkOptions =
        ForkOptions(
          workingDirectory = Some((baseDirectory in ThisBuild).value),
          bootJars         = Nil,
          javaHome         = (javaHome       in s).value,
          connectInput     = (connectInput   in s).value,
          outputStrategy   = (outputStrategy in s).value,
          runJVMOptions    = (javaOptions    in s).value,
          envVars          = (envVars        in s).value)
      new {
        val fork_ = (fork in run).value
        val config: ForkOptions = forkOptions
      } with ScalaRun {
        override def run(mainClass: String, classpath: Seq[File], options: Seq[String], log: Logger): Option[String] =
          javaRunner(
            Option(mainClass), Option(classpath), options,
            Some("java"), Option(log), fork_,
            config.runJVMOptions, config.javaHome, config.workingDirectory, config.envVars, config.connectInput, config.outputStrategy)
      }
    },
    runner  in runMain in (s) := (runner in run in (s)).value,
    run     in (s) <<= Defaults.runTask    (fullClasspath in s, mainClass in run in s, runner in run in s),
    runMain in (s) <<= Defaults.runMainTask(fullClasspath in s,                        runner in runMain in s)
  )


def javaRunner(mainClass: Option[String] = None,
               classpath: Option[Seq[File]] = None,
               options: Seq[String],
               javaTool: Option[String] = None,
               log: Option[Logger] = None,
               fork: Boolean = false,
               jvmOptions: Seq[String] = Nil,
               javaHome: Option[File] = None,
               cwd: Option[File] = None,
               envVars: Map[String, String] = Map.empty,
               connectInput: Boolean = false,
               outputStrategy: Option[OutputStrategy] = Some(StdoutOutput)): Option[String] = {

  def runner(app: String,
             args: Seq[String],
             cwd: Option[File] = None,
             env: Map[String, String] = Map.empty): Int = {
    import scala.collection.JavaConverters._

    val cmd: Seq[String] = app +: args
    val pb = new java.lang.ProcessBuilder(cmd.asJava)
    if (cwd.isDefined) pb.directory(cwd.get)
    pb.inheritIO
    //FIXME: set environment
    val process = pb.start()
    if (fork) 0
    else {
      def cancel() = {
        if(log.isDefined) log.get.warn("Background process cancelled.")
        process.destroy()
        15
      }
      try process.waitFor catch {
        case e: InterruptedException => cancel()
      }
    }
  }

  val app: String = javaHome.fold("") { p => p.absolutePath + "/bin/" } + javaTool.getOrElse("java")
  val jvm: Seq[String] = jvmOptions.map(p => p.toString)
  val cp: Seq[String] =
    classpath
      .fold(Seq.empty[String]) { paths =>
        Seq(
          "-cp",
          paths
            .map(p => p.absolutePath)
            .mkString(java.io.File.pathSeparator))
      }
  val klass = mainClass.fold(Seq.empty[String]) { name => Seq(name) }
  val xargs: Seq[String] = jvm ++ cp ++ klass ++ options

  if(log.isDefined) {
    if (fork) log.get.info("Forking:") else log.get.info("Running:")
    log.get.info(s"    ${app} " + xargs.mkString(" "))
  }

  if (cwd.isDefined) IO.createDirectory(cwd.get)
  val exitCode = runner(app, xargs, cwd, envVars)
  if (exitCode == 0)
    None
  else
    Some("Nonzero exit code returned from " + app + ": " + exitCode)
}


addCommandAlias("jsonRead",        "appJSON/RunnerJsonRead:run")
addCommandAlias("jsonWrite",       "appJSON/RunnerJsonWrite:run")


lazy val RunnerJsonRead  = sbt.config("RunnerJsonRead") .extend(Compile)
lazy val RunnerJsonWrite = sbt.config("RunnerJsonWrite").extend(Compile)





lazy val root =
  project
    .in(file("."))
    .settings(buildSettings:_*)
    .aggregate(appJSON)

lazy val appJSON =
  project
    .in(file("appJSON"))
    .settings(buildSettings:_*)
    .configs(RunnerJsonRead, RunnerJsonWrite)
    .settings(inConfig(RunnerJsonRead)(
      forkRunOptions(ThisScope) ++
        Seq(
          mainClass :=  Option("com.github.frgomes.scala.snippets.json.JsonRead"))):_*)
    .settings(inConfig(RunnerJsonWrite)(
      forkRunOptions(ThisScope) ++
        Seq(
          mainClass :=  Option("com.github.frgomes.scala.snippets.json.JsonWrite"))):_*)

