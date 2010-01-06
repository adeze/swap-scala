/* Abstract syntax of logics such as First Order Logic
 * and Intuitionistic Logic */

package org.w3.logicalsyntax

/* cf http://en.wikipedia.org/wiki/First-order_logic#Terms */
sealed case class Term
case class Variable extends Term
case class FunctionSymbol
case class Apply(f: FunctionSymbol, terms: List[Term]) extends Term {
  override def toString(): String {
    f.toString + "@" + terms.toString
  }

/* http://en.wikipedia.org/wiki/First-order_logic#Formulas */
sealed case class Formula
case class TruthConstant(b: Boolean) extends Formula
case class PredicateSymbol
case class Atom(p: PredicateSymbol, terms: List[Term]) extends Formula {
  override def toString(): String {
    p.toString() + terms.toString()
  }
}
case class Equals(x: Term, y: Term) extends Formula
case class Not(f: Formula) extends Formula
case class And(f: Formula, g: Formula) extends Formula
case class Exists(v: Variable, f: Formula)
case class Forall(v: Variable, f: Formula)

/* TODO: consider http://en.wikipedia.org/wiki/Intuitionistic_logic */
// object bottom: Formula

object Notation {
  def or(f: Formula, g: Formula) { Not(And(Not(f), Not(g))) }
  def implies(f: Formula, g: Formula) { Not(And(Not(f), g)) }

  def variables(a: Atom): List[Variable] {
    a.terms.filter(_ match { case Variable => true; case _ => false } )
  }
}

