package com.github.frgomes.scala.snippets.json

import scala.util.parsing.json._

class Data {
  val map: Map[String, Any] = // Map[String, Seq[String]]] =
    Map(
      "modules" -> Seq("ModuleA", "ModuleB"),
      "ModuleA" ->
        Map(
          "test:internalDependencyClasspath" -> Seq("ModuleA/target/classes", "ModuleA/target/test-classes"),
          "test:managedClasspath"            -> Seq("~/.ivy2/local/dep1.jar", "~/.ivy2/local/dep2.jar"),
          "test:unmanagedClasspath"          -> Seq()),
      "ModuleB" ->
        Map(
          "test:internalDependencyClasspath" -> Seq("ModuleB/target/classes", "ModuleB/target/test-classes"),
          "test:managedClasspath"            -> Seq("~/.ivy2/local/dep5.jar", "~/.ivy2/local/dep6.jar"),
          "test:unmanagedClasspath"          -> Seq()))
}

/* see: http://stackoverflow.com/questions/17521364/scala-writing-json-object-to-file-and-reading-it */
object JsonWrite extends Data {
  def main(args: Array[String]) {

    val out = if(args.length > 0 ) new java.io.PrintStream(args(0)) else new java.io.PrintStream(System.out)

    //TODO: Pretty print :: http://stackoverflow.com/questions/15718506/scala-how-to-print-case-classes-like-pretty-printed-tree
    out.println(JSONObject(map).toString())
  }
}


/** see: http://stackoverflow.com/questions/4170949/how-to-parse-json-in-scala-using-standard-scala-classes */
object JsonRead extends Data {
  def main(args: Array[String]) {
    val in = if(args.length > 0 ) new java.io.FileInputStream(args(0)) else System.in
    val lines = scala.io.Source.fromInputStream(in, "UTF-8").getLines
    //XXX lines.foreach(line => println(line))

    class CC[T] { def unapply(a:Any):Option[T] = Some(a.asInstanceOf[T]) }

    object M extends CC[Map[String, Any]]
    object L extends CC[List[String]]
    object S extends CC[String]
    object D extends CC[Double]
    object B extends CC[Boolean]

    val x =
    (for {
      Some(M(map)) <- Seq(JSON.parseFull(lines.mkString))
      L(modules) = map("modules")
      S(module) <- modules
      // M(paths) = map(module)
      // L(i) = paths("test:internalDependencyClasspath")
      // L(m) = paths("test:managedClasspath")
      // L(u) = paths("test:unmanagedClasspath")
    } yield {
      (module /*, i, m, u */)
    })

      x.foreach {
        case (module: String /*, i, m, u */) =>
          println(s"$module")
          // println(s"    test:internalDependencyClasspath = $i")
          // println(s"    test:managedClasspath = $m")
          // println(s"    test:unmanagedClasspath = $u")
      }
  }
}
