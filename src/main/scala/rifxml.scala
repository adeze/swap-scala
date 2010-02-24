package org.w3.swap.rifxml

import org.w3.swap

/**
 * <a href="http://www.w3.org/TR/rif-bld/#Direct_Specification_of_RIF-BLD_Presentation_Syntax"
 * >2 Direct Specification of RIF-BLD Presentation Syntax</a>
 */
trait RIFSyntax {
  type Term
  type BaseTerm <: Term
  /**
   * positional or named-argument term
   */
  type ArgTerm <: BaseTerm
  type Const <: BaseTerm
  type Var <: BaseTerm
  type ArgNames

  /*
  #  connective symbols And, Or, and :-
  # quantifiers Exists and Forall
  # the symbols =, #, ##, ->, External, Import, Prefix, and Base
  # the symbols Group and Document
  # the symbols for representing lists: List and OpenList.
  # the auxiliary symbols (, ), [, ], <, >, and ^^ 
  */

  type PredicateSymbol <: Const
  type FunctionSymbol <: Const

  type Formula
  type ConditionFormula <: Formula
  type ConjunctionOfAtoms <: ConditionFormula
  type AtomicFormula <: ConjunctionOfAtoms
  type RuleImplication <: Formula

  type Directive
}

trait RIFBuilder extends RIFSyntax {
  def data(literal: String, symspace: String): Const

  def positional_term(fun: Const, args: Seq[BaseTerm]): ArgTerm
  def named_arguments_term(t: Const, args: Map[ArgNames, BaseTerm]): ArgTerm
  def closed_list(items: List[Term]): Term
  def open_list(items: List[Term], tail: Term): Term
  def external_term(t: ArgTerm): Term

  def equality_term(t: BaseTerm, s: BaseTerm): AtomicFormula
  def membership_term(t: BaseTerm, s: BaseTerm): AtomicFormula
  def subclass_term(t: BaseTerm, s: BaseTerm): AtomicFormula
  def frame_term(t: BaseTerm, args: Map[BaseTerm, BaseTerm]): AtomicFormula

  def atomic_formula(p: PredicateSymbol, args: Seq[BaseTerm]): AtomicFormula
  def external_atomic(a: AtomicFormula): AtomicFormula

  def conjunction(phi1n: Seq[ConditionFormula]): ConditionFormula
  def disjunction(phi1n: Seq[ConditionFormula]): ConditionFormula
  def existential(v1n: Set[Var], phi: ConditionFormula): ConditionFormula

  /**
   * @param phi: none of the atomic formulas in phi is
   *             an externally defined term
   */
  def implication(phi: ConjunctionOfAtoms,
		  psi: ConditionFormula): RuleImplication

  /**
   * @param phi: constraint: freevars(phi) subset v1n
   */
  def universal_rule(v1n: Set[Var], phi: RuleImplication)

  /**
   * @param phi: constraint: freevars(phi) subset v1n
   */
  def universal_fact(v1n: Set[Var], phi: AtomicFormula)

  /**
   * If phi1, ..., phin are RIF-BLD rules, universal facts,
   * variable-free rule implications, variable-free atomic formulas,
   * or group formulas then Group(phi1 ... phin) is a group formula.
   * As a special case, the empty group formula, Group(), is allowed
   * and is treated as a tautology, i.e., a formula that is always true. 
   */
  def group(phi1n: Seq[Formula]): Formula

  def document(base: Option[String],
	       prefix_directives: Seq[(String, String)],
	       import_directives: Seq[(String, Option[String])],
	       directive1n: Seq[Directive], gamma: Option[Formula]): Formula
}
