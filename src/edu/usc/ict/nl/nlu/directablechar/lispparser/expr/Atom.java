package edu.usc.ict.nl.nlu.directablechar.lispparser.expr;




public class Atom implements Expr
{
    String name;
    public String toStringFlat()
    {
        return name;
    }
    @Override
    public String toString() {
    	return toStringFlat();
    }
    
    public Atom(String text)
    {
        name = text;
    }
	@Override
	public Expr car() {
		return null;
	}
	@Override
	public Expr nth(int index) {
		return null;
	}
	@Override
	public ExprList list() {
		ExprList ret=new ExprList();
		ret.add(this);
		return ret;
	}
	public void setName(String name) {
		this.name = name;
	}
 
}