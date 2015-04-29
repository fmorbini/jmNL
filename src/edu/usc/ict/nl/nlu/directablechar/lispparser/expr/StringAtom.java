package edu.usc.ict.nl.nlu.directablechar.lispparser.expr;



public class StringAtom extends Atom
{
    public String toStringFlat()
    {
        // StreamTokenizer hardcodes escaping with \, and doesn't allow \n inside words
        String escaped = name.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"");
        return "\""+escaped+"\"";
    }
    @Override
    public String toString() {
    	return toStringFlat();
    }
 
    public StringAtom(String text)
    {
        super(text);
    }
    public String getValue()
    {
        return name;
    }
}