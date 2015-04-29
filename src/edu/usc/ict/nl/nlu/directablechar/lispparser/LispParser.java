package edu.usc.ict.nl.nlu.directablechar.lispparser;

import edu.usc.ict.nl.nlu.directablechar.lispparser.expr.Atom;
import edu.usc.ict.nl.nlu.directablechar.lispparser.expr.Expr;
import edu.usc.ict.nl.nlu.directablechar.lispparser.expr.ExprList;
import edu.usc.ict.nl.nlu.directablechar.lispparser.expr.StringAtom;

public class LispParser
{
	LispTokenizer tokenizer;

	public LispParser(LispTokenizer input)
	{
		tokenizer=input;
	}

	public class ParseException extends Exception
	{

	}

	public Expr parseExpr() throws ParseException
	{
		Token token = (tokenizer.hasNext())?tokenizer.next():null;
		if (token!=null) {
			switch(token.type)
			{
			case '(': return parseExprList(token);
			case '"': return new StringAtom(token.text);
			default: return new Atom(token.text);
			}
		}
		else return null;
	}


	protected ExprList parseExprList(Token openParen) throws ParseException
	{
		ExprList acc = new ExprList();
		while(tokenizer.peekToken().type != ')')
		{
			Expr element = parseExpr();
			acc.add(element);
		}
		Token closeParen = tokenizer.next();
		return acc;
	}

}