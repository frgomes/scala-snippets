package com.github.frgomes.scala.snippets.json


class Data {
  val list = Seq("a", "b")
  val map : Map[String, Map[String, Seq[String]]] =
    Map(
      "project" ->
        Map(
          "modules" -> Seq("ModuleA", "ModuleB")),
      "ModuleA" ->
        Map(
          "test:internalDependencyClasspath" -> Seq("ModuleA/target/classes", "ModuleA/target/test-classes"),
          "test:managedClasspath"            -> Seq("~/.ivy2/local/dep1/dep1.jar", "~/.ivy2/local/dep2/dep2.jar"),
          "test:unmanagedClasspath"          -> Seq()),
      "ModuleB" ->
        Map(
          "test:internalDependencyClasspath" -> Seq("ModuleB/target/classes", "ModuleB/target/test-classes"),
          "test:managedClasspath"            -> Seq("~/.ivy2/local/dep5/dep5.jar", "~/.ivy2/local/dep6/dep6.jar"),
          "test:unmanagedClasspath"          -> Seq()))
}


// see: http://stackoverflow.com/questions/4170949/how-to-parse-json-in-scala-using-standard-scala-classes
class CC[T] { def unapply(a:Any):Option[T] = Some(a.asInstanceOf[T]) }
object MM extends CC[Map[String, Map[String, Seq[String]]]]
object M  extends CC[Map[String, Seq[String]]]
object L  extends CC[Seq[String]]
object S  extends CC[String]
object D  extends CC[Double]
object B  extends CC[Boolean]


object JsonWrite extends Data {
  def main(args: Array[String]) {
    val out = if(args.length > 0 ) new java.io.PrintStream(args(0)) else new java.io.PrintStream(System.out)

    implicit val formats = org.json4s.DefaultFormats
    import org.json4s.native.Serialization.{write => swrite}
    out.println(swrite(map))
  }
}


object JsonRead extends Data {
  def main(args: Array[String]) {
    val in = if(args.length > 0 ) new java.io.FileInputStream(args(0)) else System.in
    val text = scala.io.Source.fromInputStream(in, "UTF-8").getLines.mkString

    implicit val formats = org.json4s.DefaultFormats
    import org.json4s.native.Serialization.{read => sread}
    //XXX println(text)
    //XXX println(sread[Map[String, Map[String, Seq[String]]]](text))
    //XXX println(x3)

    (for {
      MM(map)    <- Seq(sread[Map[String, Map[String, Seq[String]]]](text))
      M(project) <- Seq(map("project"))
      L(modules) <- Seq(project("modules"))
      S(module)  <- modules
      M(paths)   <- Seq(map(module))
      L(i)       <- Seq(paths("test:internalDependencyClasspath"))
      L(m)       <- Seq(paths("test:managedClasspath"))
      L(u)       <- Seq(paths("test:unmanagedClasspath"))
    } yield {
      (module, i, m, u)
    })
      .foreach {
        case (module, i, m, u) =>
          println(s"$module")
          println(s"    test:internalDependencyClasspath = $i")
          println(s"    test:managedClasspath = $m")
          println(s"    test:unmanagedClasspath = $u")
      }
  }
}
