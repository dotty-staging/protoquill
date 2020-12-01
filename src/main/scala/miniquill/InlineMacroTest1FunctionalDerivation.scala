package miniquill

import simple.SimpleMacro._
import scala.language.implicitConversions
import miniquill.quoter.Dsl._
import scala.compiletime.{erasedValue, summonFrom, constValue}
import miniquill.quoter.QueryDsl._

object InlineMacroTest1FunctionalDerivation {
  import io.getquill._
  val ctx = new MirrorContext(MirrorSqlDialect, Literal)
  import ctx._
  import io.getquill.derived.MapProc

  case class Person(firstName: String, lastName: String, age: Int)

  val values: Map[String, String] = Map("firstName" -> "Joe", "age" -> "22")  

  inline def q = quote {
    query[Person].filter(p => 
      MapProc[Person, PrepareRow](p, values, null, (a, b) => (a == b) || (b == (null) ) )
    )
  }
  /* 
   ============= The following expasion happens ===========
   SELECT p.firstName, p.lastName, p.age FROM Person p 
   WHERE 
     ( p.firstName = [ values.getOrElse("firstName",null) ] OR [ values.getOrElse("firstName",null) ] == null ) AND 
     ( p.lastName = [ values.getOrElse("lastName",null) ] OR [ values.getOrElse("lastName",null) ] == null ) AND
     ( p.age = [ values.getOrElse("age",null) ] OR [ values.getOrElse("age",null) == null ] ) AND true
  */























  //       //MapProc[Person, PrepareRow](p, values, "%", (a, b) => a.like(b) )
  //MapProc[Person, PrepareRow](p, values, null, (a, b) => (a == b) || (b == (null: String) ) )
  

  def main(args: Array[String]): Unit = {
    val r = run(q) 
    println(r.string)
    println(r.prepareRow.data.toList)
  }
  
}
