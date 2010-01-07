/*
 * RDF Abstract syntax as per 2004 Recommendation
 */

package org.w3.swap.rdf2004

import org.w3.swap.logicalsyntax.{Formula, TruthConstant, Atom, And, Exists,
				  PredicateSymbol,
				  Term, Apply, Variable,
				  FunctionSymbol,
				  Notation}

/* Terms */
case class URI(i: String) /* ISSUE: not every string makes a URI */
          /* ISSUE: actually closer to IRI; called RDFuri or something
	   * in the spec */
     extends FunctionSymbol(0) {
  override def toString(): String = {
    "<" + i + ">"
  }
}
sealed abstract class Literal() extends FunctionSymbol(0)
case class PlainLiteral(s: String) extends Literal
case class Language(code: String) /* ISSUE: restricted to lang code syntax */
case class Text(chars: String, lang: Language) extends Literal {
  override def toString(): String = {
    "'" + chars + "'@" + lang.code
  }
}
case class DatatypedLiteral(value: String, dt: URI) extends Literal {
  override def toString(): String = {
    "'" + value + "'^^<" + dt.i + ">"
  }
}


case class BlankNode(hint: String, id: AnyRef) extends Variable {
  override def toString(): String = {
    "_:" + hint
  }
}

case class R(name: String, override val arity: Int) extends
  PredicateSymbol(arity)

class SyntaxError(msg: String) extends Exception

/* Formulas */
object AbstractSyntax {
  /* ISSUE: follow ACL2 in having = as the only relation symbol?
   * i.e. use a (holds ...) -> T/NIL function rather than
   * a holds predicate? This would get rid of TruthConstant() too. */
  val holds = R("holds", 3)

  /* checks well-formedness of Atoms */
  def triple(s: Term, p: Term, o: Term) = {
    p match {
      case Apply(_: URI, Nil) =>
	s match {
	  /* is there a scala syntax for folding 2 cases? */
	  case BlankNode(_, _) => Atom(holds, List(s, p, o))
	  case Apply(_: URI, Nil) => Atom(holds, List(s, p, o))
	  case _ => throw new SyntaxError("subject must be URI or Blank Node")
	}
      case _ => throw new SyntaxError("predicate must be URI")
    }
  }

  def variables(atoms: List[Atom]): List[Variable] = {
    atoms.flatMap(a => Notation.variables(a))
  }

  def quantify(f: Formula, vars: List[Variable]): Formula = {
    if (vars == Nil) { f }
    else { quantify(Exists(vars.head, f), vars.tail) }
  }

  def unquantify(f: Formula): (List[Variable], Formula) = {
    f match {
      case Exists(v, g) => {
	val (rest, gg) = unquantify(g)
	(v :: rest, gg)
      }
      case _ => (List(), f)
    }
  }

  def graph(triples: List[Atom]): Formula = {
    if (triples == Nil) { TruthConstant(true) }
    else {
      val sorted = triples.sort((a, b) => (compare(a, b) <= 0))
      
      def conjoin(todo: List[Atom], done: Formula): Formula = {
	if (todo == Nil) { done }
	else { conjoin(todo.tail, And(todo.head, done)) }
      }
      
      quantify(conjoin(sorted.tail, sorted.head), variables(triples))
    }
  }

  val graph0 = TruthConstant(true)

  def add (graph: Formula, s: Term, p: Term, o: Term): Formula = {
    val t = triple(s, p, o)
    if (graph == graph0) { t }
    else {
      val (vars, conjunction) = unquantify(graph)
      quantify(And(t, conjunction), vars union Notation.variables(t))
    }
  }

  /*
   * ISSUE: keep the triples sorted for ease of graph comparison?
   */
  def compare (x: Atom, y: Atom): Int = { x.hashCode().compare(y.hashCode()) }

}

