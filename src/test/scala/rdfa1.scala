package org.w3.swap.test

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

import org.w3.swap
import swap.rdflogic.{TermNode, Name}
import swap.rdfa
import swap.rdfxml

object RDFaParser extends rdfa.RDFaSyntax with TermNode {
  type BlankNode = rdfxml.XMLVar

  lazy val vars = new rdfxml.Scope()
  def fresh(hint: String) = vars.fresh(hint)
  def byName(name: String) = vars.byName(name)
}

class RDFaMisc extends Spec with ShouldMatchers {

  describe("RDFa walker") {

    it("should stop chaining on bogus rel values (Test #105) ") {
      val e1 = <div
      xmlns:dc="http://purl.org/dc/elements/1.1/"
      about="" 
      rel="dc:creator">
      <a rel="myfoobarrel" href="ben.html">Ben</a> created this page.
      </div>

      var addr = "data:"
      val undef = RDFaParser.undef
      var arcs = RDFaParser.walk(e1, addr, Name(addr), undef, Nil, Nil, null)
      (arcs.force.head match {
	case (Name(_), Name(_), rdfxml.XMLVar(_, _)) => true
	case _ => false
      }) should equal (true)
    }
  }
}
