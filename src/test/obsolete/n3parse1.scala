package org.w3.swap.test

/* tests for N3 parsing
 * todo: try http://www.w3.org/2000/10/swap/test/n3parser.tests
 * */

import org.scalacheck._
import Prop._
import Arbitrary.arbitrary

import org.w3.swap

object numberLex extends Properties("N3 tokenization") {
  import swap.n3.{N3Lex, QName}

  case class IO(in: String, out: List[Any])

  def dec(s: String) = new java.math.BigDecimal(s)

  val expectedNum: List[IO] = List(
    // TODO: what about really big integers?

    /* scala note: List(...) alone allows ints to be converted
     * to doubles, so we have to use List[Any](...)
     * Thanks to paulp_ in #scala on FreeNode for the clue.*/

    IO("1 234e0 1.2",
       List[Any](1, 234.0, dec("1.2"))),
    IO("  982739\t234e0 132432432432432432.2324",
       List[Any](982739, 234.0, dec("132432432432432432.2324")) ),

    IO("-982739 +93287 234.234e3 -234e3",
       List[Any](-982739, +93287, 234.234e3, -234e3) ),
    IO("234e0 +234e34 +234e-34 234e-34 234e-34",
       List[Any](234e0, +234e34, +234e-34, 234e-34, 234e-34) ),
    IO("1324.2324 234e+34 -1.2 +1.2",
       List[Any](dec("1324.2324"), 234.0e+34, dec("-1.2"), dec("+1.2")))
  )

  val expectedOther: List[IO] = List(
    IO("<abc> foo:bar ?x _:y",
       List[Any]("abc", QName("foo", "bar"), "x", "y") ),
    IO("<abc#def> foo:bar 123",
       List[Any]("abc#def", QName("foo", "bar"), 123) )
    )

  class N3TokenList extends N3Lex {
    /* darn... to implement the longest-matching rule, this is order-sensitive*/
    def tokens: Parser[List[Any]] = rep(
      double | decimal | integer
      | uriref | qname
      | evar | uvar
    )
  }

  def lexTest(io: IO): Boolean = {
    val lexer = new N3TokenList()
    val result = lexer.parseAll(lexer.tokens, io.in)

    result match {
      case lexer.Success(tokens, _ ) => tokens == io.out
      case lexer.Failure(_, _) | lexer.Error(_, _) => false
    }
  }

  /* scala note:
   * Gen.oneOf() takes repeated parameters; _* lets us call
   * it on a list. */
  property ("numerals tokenize correctly") =
    Prop.forAll(Gen.oneOf(expectedNum.map(Gen.value): _*)){
      io => lexTest(io)
    }


  property ("other tokens tokenize correctly") =
    Prop.forAll(Gen.oneOf(expectedOther.map(Gen.value): _*)){
      io => lexTest(io)
    }
}

object n3parsing extends Properties("N3 Parsing") {
  import org.w3.swap.logic.{Formula, Exists, And, Apply, Literal}
  import org.w3.swap.rdf.{URI, BlankNode, Holds}
  import swap.n3.AbstractSyntax.statement
  import org.w3.swap.n3.N3Parser

  case class IO(in: String, out: Formula)

  def ioProp(expected: List[IO]) =
    Prop.forAll(Gen.oneOf(expected.map(Gen.value): _*)){ io =>
      val p = new N3Parser("data:")
      val result = p.parseAll(p.document, io.in)

      result match {
	case p.Success(f, _) => {
	  ("wrong parse result: " + f.toString) |: f == io.out
	}
	case p.Failure(x, rest) => {
	  ("failure: " + rest.pos.longString) |: false
	}
	case p.Error(x, rest) => {
	  ("Error: " + rest.toString()) |: false
	}
      }
    }

  property ("empty document parses to the true formula, (and)") =
    ioProp(List( IO("", And(List())) ))

  property ("simple statements of 3 URI ref terms work") =
    ioProp(List(
      IO("<#pat> <#knows> <#joe>.",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#knows"),
			    URI("data:#joe") ))) ),
      IO("<#pat> has <#brother> <#joe>.",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#brother"),
			    URI("data:#joe") ))) )
    ))

  property ("comments work like whitespace") =
    ioProp(List(
      IO("#neener", And(List())),
      IO("@prefix : <#>. #abc", And(List())),
      IO("<#pat> <#knows> <#joe>. #yay!",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#knows"),
			    URI("data:#joe") )))
       ) ))
	 

  property ("integer, string literals work as objects, subjects") =
    ioProp(List(
      IO("<#pat> <#age> 23.",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#age"),
			    Literal(23) )))
       ),
      IO("22 <#lessThan> 23.",
	 And(List(statement(Literal(22),
			    URI("data:#lessThan"),
			    Literal(23) )))
       ),
      IO("<#pat> <#name> \"Pat\".",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#name"),
			    Literal("Pat") )))
       ),
      IO("<#pat> <#name> \"\"\"Pat\"\"\".",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#name"),
			    Literal("Pat") )))
       ),
      IO("<#pat> <#name> \"\"\"'data:text/rdf+n3;charset=utf-8;base64,QHByZWZpeCBsb2c6IDxodHRwOi8vd3d3LnczLm9yZy8yMDAwLzEwL3N3YXAvbG9nIz4gLgp7fSA9PiB7OmEgOmIgOmN9IC4g'\"\"\".",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#name"),
			    Literal("'data:text/rdf+n3;charset=utf-8;base64,QHByZWZpeCBsb2c6IDxodHRwOi8vd3d3LnczLm9yZy8yMDAwLzEwL3N3YXAvbG9nIz4gLgp7fSA9PiB7OmEgOmIgOmN9IC4g'") )))
       )
      ))

  property ("is/of inverts sense of properties") =
    ioProp(List(
      IO("23 is <#age> of <#pat>.",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#age"),
			    Literal(23) )))
       ) ))

  property ("empty prefix decl") =
    ioProp(List(
      IO("@prefix : <#>. :pat :knows :joe.",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#knows"),
			    URI("data:#joe") ))) ),
      IO("@prefix : <#>. :pat :knows :joe-joe.",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#knows"),
			    URI("data:#joe-joe") ))) ),
      IO("@prefix : <http://example/vocab#>. :pat :knows :joe.",
	 And(List(statement(URI("http://example/vocab#pat"),
			    URI("http://example/vocab#knows"),
			    URI("http://example/vocab#joe") ))) )
      ))

  property ("document with 2 statements works") =
    ioProp(List(
      IO("<#pat> <#age> 23. <#pat> <#name> \"Pat\".",
	 And(List(statement(URI("data:#pat"),
			    URI("data:#age"),
			    Literal(23) ),
		  statement(URI("data:#pat"),
			    URI("data:#name"),
			    Literal("Pat") ) )) )
    ))

  /** hmm... BlankNode qualifiers are no longer so predictable.
  property ("[] terms parse as bnodes with properties") =
    ioProp(List(
      IO("<#pat> <#child> [ <#age> 4 ].",
	 {
	   val e1 = BlankNode("something", Some(10))
	   def i(s: String): URI = URI("data:#" + s)
	   Exists(Set(e1),
		  And(List(statement(e1, i("age"), Literal(4)),
			   statement(i("pat"), i("child"), e1) )) )
	 })
      ))
   */

    /* for later...
    IO("""@prefix : <#>.
       @prefix owl: <data:owl#>.
       @prefix rdfs: <data:rdfs#>.
       { ?pizza :toppings ?L.
       ?C owl:oneOf ?L.
       ?C rdfs:subClassOf [ owl:complementOf :MeatToppings] }
       => { ?pizza a :VegetarianPizza }""",
       Forall(List(Var("pizza"), Var("L"), Var("C")),
	      Implies(
		Exists(Exists(List(BlankNode("", Some((6, 20)))),
			      And(atom(Var("pizza"),
				       URI("data:#toppings"),
				       Var("L") ),
				  atom(Var("C")
				       URI("data:owl#oneOf"),
				       Var("L") ),
				  ...
     */
}
