package edu.usc.ict.nl.nlu.directablechar.lispparser.expr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
 
public class ExprList extends ArrayList<Expr> implements Expr
{
    ExprList parent = null;
    int indent =1;
 
    public int getIndent()
    {
        if (parent != null)
        {
            return parent.getIndent()+indent;
        }
        else return 0;
    }
 
    public void setIndent(int indent)
    {
        this.indent = indent;
    }
 
 
 
    public void setParent(ExprList parent)
    {
        this.parent = parent;
    }
 
    public String toString()
    {
        String indent = "";
        if (parent != null && parent.get(0) != this)
        {
            indent = "\n";
            char[] chars = new char[getIndent()];
            Arrays.fill(chars, ' ');
            indent += new String(chars);		
        }
 
        String output = indent+"(";
        for(Iterator<Expr> it=this.iterator(); it.hasNext(); ) 
        {
            Expr expr = it.next();
            output += expr.toString();
            if (it.hasNext())
                output += " ";
        }
        output += ")";
        return output;
    }
    public String toStringFlat() {
        String output = "(";
        for(Iterator<Expr> it=this.iterator(); it.hasNext(); ) 
        {
            Expr expr = it.next();
            output += expr.toStringFlat();
            if (it.hasNext())
                output += " ";
        }
        output += ")";
        return output;
    }
 
    @Override
    public synchronized boolean add(Expr e)
    {
        if (e instanceof ExprList)
        {
            ((ExprList) e).setParent(this);
            if (size() != 0 && get(0) instanceof Atom)
                ((ExprList) e).setIndent(2);
        }
        return super.add(e);
    }

	@Override
	public Expr car() {
		return get(0);
	}

	@Override
	public Expr nth(int index) {
		return get(index);
	}

	public ExprList cdr() {
		ExprList ret=new ExprList();
		boolean first=true;
		for(Expr x:this) {
			if (!first) ret.add(x);
			else first=false;
		}
		return ret;
	}
	
	public ExprList butLast() {
		ExprList ret=new ExprList();
		int l=size();
		for(int i=0;i<l-1;i++) {
			ret.add(get(i));
		}
		return ret;
	}
	public Expr getLast() {
		int l=size();
		return get(l-1);
	}
	@Override
	public ExprList list() {
		ExprList ret=new ExprList();
		ret.add(this);
		return ret;
	}
}