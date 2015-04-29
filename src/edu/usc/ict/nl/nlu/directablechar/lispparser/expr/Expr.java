package edu.usc.ict.nl.nlu.directablechar.lispparser.expr;



public interface Expr {

	public Expr car();
	public Expr nth(int index);
	public ExprList list();
	public String toStringFlat();
}


