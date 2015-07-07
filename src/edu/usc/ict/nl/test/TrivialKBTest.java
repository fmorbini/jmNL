package edu.usc.ict.nl.test;

import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.kb.cf.CustomFunctionInterface;
import edu.usc.ict.nl.kb.cf.TestRewardDM;
import edu.usc.ict.nl.utils.FloatAndLongUtils;


public class TrivialKBTest extends TestCase {
	
	public static Object evaluate(DialogueKBInterface kb,DialogueKBFormula f) throws Exception {
		return kb.evaluate(f, null);
	}
	
	// null assignments
	public void test1() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		Boolean r=mykb.evaluate(e);
		assertTrue(r==null);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		r=mykb.evaluate(e);
		assertTrue(r==true);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null).negate());
		r=mykb.evaluate(e);
		assertTrue(r==false);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),null);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		r=mykb.evaluate(e);
		assertTrue(!r);
	}
	//test of case sensitivity setup
	public void test14() throws Exception {
		NLBusConfig config = new NLBusConfig();
		config.setCaseSensitive(true);
		DM dm=new TestRewardDM(config);
		TrivialDialogueKB mykb = new TrivialDialogueKB(dm);
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),2);
		DialogueKBFormula f=parseWithCheck("+(A,1)");
		Object r=evaluate(mykb,f);
		assertTrue(r==null);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		r=evaluate(mykb,f);
		assertTrue(r==null);
		config.setCaseSensitive(false);
		f=parseWithCheck("+(A,1)");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (Long)r==3);
		
		config.setCaseSensitive(true);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("A", null),-1);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),parseWithCheck("+(A,1)"));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("a");
		r=evaluate(mykb,f);
		assertTrue(r!=null && ((Long)r)==0);
		f=parseWithCheck("A");
		r=evaluate(mykb,f);
		assertTrue(r!=null && ((Long)r)==-1);
		
		e=DialogueOperatorEffect.parse("assign(d,'low')");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("d");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof String) && r.equals("'low'"));
		e=DialogueOperatorEffect.parse("assign(g,d)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("g");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof String) && r.equals("'low'"));
		e=DialogueOperatorEffect.createAssignment("g", r);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof String) && r.equals("'low'"));
		f=parseWithCheck("eq(d,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && (Boolean)r);
		f=parseWithCheck("eq(d,'Low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && !(Boolean)r);
		f=parseWithCheck("eq(D,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && !(Boolean)r);
		f=parseWithCheck("Eq(D,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && !(Boolean)r);
		f=parseWithCheck("EQ(D,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && !(Boolean)r);
		f=parseWithCheck("Eq(d,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && (Boolean)r);
		f=parseWithCheck("EQ(d,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && (Boolean)r);
	}
	// evaluation of assignment values
	public void test2() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),2);
		DialogueKBFormula f=parseWithCheck("+(a,1)");
		Object r=evaluate(mykb,f);
		assertTrue(r==null);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		r=evaluate(mykb,f);
		assertTrue(r!=null && ((Long)r)==3);
		f=parseWithCheck("+(b,1)");
		r=evaluate(mykb,f);
		assertTrue(r==null);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),parseWithCheck("+(a,1)"));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("+(a,1)");
		r=evaluate(mykb,f);
		assertTrue(r!=null && ((Long)r)==4);
		f=parseWithCheck("+(A,1)");
		r=evaluate(mykb,f);
		assertTrue(r!=null && ((Long)r)==4);
		e=DialogueOperatorEffect.parse("assign(d,'low\\'high')");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("d");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof String) && r.equals("'low\\'high'"));
		e=DialogueOperatorEffect.parse("assign(d,'low')");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("d");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof String) && r.equals("'low'"));
		e=DialogueOperatorEffect.parse("assign(g,d)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("g");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof String) && r.equals("'low'"));
		e=DialogueOperatorEffect.createAssignment("g", r);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof String) && r.equals("'low'"));
		f=parseWithCheck("eq(d,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && (Boolean)r);
		f=parseWithCheck("eq(d,'Low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && !(Boolean)r);
		f=parseWithCheck("eq(D,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && (Boolean)r);
		f=parseWithCheck("Eq(D,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && (Boolean)r);
		f=parseWithCheck("EQ(D,'low')");
		r=evaluate(mykb,f);
		assertTrue(r!=null && (r instanceof Boolean) && (Boolean)r);

		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),2);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),parseWithCheck("*(a,2)"));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		r=mykb.get("a");
		assertTrue(r!=null && r.equals(4l));
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),parseWithCheck("/(a,2)"));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		r=mykb.get("a");
		assertTrue(r!=null && r.equals(2l));

		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),2);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		Object v = mykb.getValueOfVariable("a",ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
		assertEquals(v, 2l);
		assertTrue(mykb.hasVariableNamed("a",ACCESSTYPE.AUTO_OVERWRITEAUTO));
		v = evaluate(mykb,DialogueKBFormula.create("a",null));
		assertEquals(v, 2l);
		
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),DialogueKBFormula.create("'b'", null));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		v = mykb.getValueOfVariable("a",ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
		assertEquals(v, "'b'");
		assertTrue(mykb.hasVariableNamed("a",ACCESSTYPE.AUTO_OVERWRITEAUTO));
		v = evaluate(mykb,DialogueKBFormula.create("a",null));
		assertEquals(v, "'b'");
		
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),true);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		v = mykb.getValueOfVariable("a",ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
		assertEquals(v, true);
		assertTrue(mykb.hasVariableNamed("a",ACCESSTYPE.AUTO_OVERWRITEAUTO));
		v = evaluate(mykb,DialogueKBFormula.create("a",null));
		assertEquals(v, true);
		
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),null);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		v = mykb.getValueOfVariable("a",ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
		assertEquals(v, null);
		assertTrue(mykb.hasVariableNamed("a",ACCESSTYPE.AUTO_OVERWRITEAUTO));
		v = evaluate(mykb,DialogueKBFormula.create("a",null));
		assertEquals(v, null);

		e=DialogueOperatorEffect.parse("assign(a,1)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.parse("++(a)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("==(a,2)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		e=DialogueOperatorEffect.parse("Assign(A,7)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.parse("++(a)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("==(a,8)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		e=DialogueOperatorEffect.parse("Assign(A,2)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.parse("++a");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("==(a,3)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		e=DialogueOperatorEffect.parse("++(a,2)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("==(a,5)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		e=DialogueOperatorEffect.parse("++(a,a)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("==(a,10)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		e=DialogueOperatorEffect.parse("x='LA'");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("x=='LA'");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
	}
	// comparisons
	public void test3() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("a", null),2);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("event", null),DialogueKBFormula.parse("'abc'"));
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		DialogueKBFormula f=parseWithCheck(">(a,1)");
		Object r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck(">=(a,2)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("a>=2");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("==(a,2)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("a==2");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("==(a,2.0)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("a!=3.0");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("!(a==3.0)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("~(a==3.0)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("eq(a,2.0)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("EQ(a,2.0)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("<(a,3)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("a<3");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("lt(a,3)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("<=(a,3)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("a<=3");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("le(a,3)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("==(event,'abc')");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
	}
	// boolean assignments
	public void test4() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),false);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		Boolean r=mykb.evaluate(e);
		assertTrue(r==false);

		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),true);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		r=mykb.evaluate(e);
		assertTrue(r==true);

		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),null);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		r=mykb.evaluate(e);
		assertTrue(!r);

		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		r=mykb.evaluate(e);
		assertTrue(r==true);

		e=DialogueOperatorEffect.parse("assign(a,isQuestion('a'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		DialogueKBFormula f=DialogueKBFormula.parse("a");
		Object rb=evaluate(mykb,f);
		assertTrue(!(Boolean) rb);
		e=DialogueOperatorEffect.parse("assign(a,isQuestion('question.a'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue((Boolean) rb);

		e=DialogueOperatorEffect.parse("assign(a,match('a','a'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue((Boolean) rb);
		e=DialogueOperatorEffect.parse("assign(a,match('a','a*'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue((Boolean) rb);
		e=DialogueOperatorEffect.parse("assign(a,match('a','b'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue(!(Boolean) rb);
		e=DialogueOperatorEffect.parse("assign(a,match('aab','[ab]+'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue((Boolean) rb);
		e=DialogueOperatorEffect.parse("assign(a,match('answer.observable.nightmares','answer\\.observable.*'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue((Boolean) rb);
		e=DialogueOperatorEffect.parse("assign(a,match('answer.observable','answer\\.observable.*'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue((Boolean) rb);
		e=DialogueOperatorEffect.parse("assign(e,'answer.observable.wakeup-nightmare')");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.parse("assign(a,match(e,'answer\\.observable.*'))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		f=DialogueKBFormula.parse("a");
		rb=evaluate(mykb,f);
		assertTrue((Boolean) rb);
		
		HashMap<String, CustomFunctionInterface> fns = DialogueKBFormula.getCustomfunctions();
		if(fns!=null) {
			for(CustomFunctionInterface fn:fns.values()) {
				boolean result=fn.test();
				System.out.println(fn.getName()+" test method has returned: "+result);
				assertTrue(result);
			}
		}
	}
	// inheritance
	public void test5() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		TrivialDialogueKB mykb2 = new TrivialDialogueKB(mykb);
		TrivialDialogueKB mykb3 = new TrivialDialogueKB(mykb2);
		DialogueOperatorEffect et=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		mykb.store(et, ACCESSTYPE.AUTO_OVERWRITETHIS, false);
		DialogueOperatorEffect ef=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null).negate());
		mykb2.store(ef, ACCESSTYPE.AUTO_OVERWRITETHIS, false);
		Boolean r=mykb3.evaluate(et);
		assertTrue(r==false);
		r=mykb3.evaluate(ef);
		assertTrue(r==true);
		r=mykb2.evaluate(ef);
		assertTrue(r==true);
		r=mykb.evaluate(ef);
		assertTrue(r==false);
	}
	// complex formulas
	public void test6() throws Exception {
		DialogueKBFormula ff1=DialogueKBFormula.parse("and(a,b,c)");
		DialogueKBFormula ff2=DialogueKBFormula.parse("and(a,b)");
		assertTrue(ff1!=ff2);
		ff1=DialogueKBFormula.parse("and(a,b)");
		assertTrue(ff1==ff2);
		ff1=DialogueKBFormula.parse("and(a,b,c)");
		ff2=DialogueKBFormula.parse("and(a,b,c)");
		assertTrue(ff1==ff2);
		
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssertion(parseWithCheck("a"));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.createAssertion(parseWithCheck("~b"));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.createAssignment(parseWithCheck("c"),2);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.createAssignment(parseWithCheck("d"),false);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.createAssignment(parseWithCheck("e"),"true");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		DialogueKBFormula f=parseWithCheck("and(a,~b)");
		Object r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("and(a,b)");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);

		f=parseWithCheck("and(a,true)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		
		f=parseWithCheck("true");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		
		f=parseWithCheck("and(a,or(b,==(c,2)))");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("and(a,known(c))");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("and(a,~known(c))");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);

		f=parseWithCheck("and(a,~known(f))");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("==(f,null)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("==(a,true)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("not(==(b,true))");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("==(b,false)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("and(a,~b,==(c,2),~d,e)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		
		e=DialogueOperatorEffect.createAssignment(parseWithCheck("e"),"'question.test'");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("isQuestion(e)");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("isQuestion('question')");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("isQuestion('question.test')");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("isQuestion('test')");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);
		f=parseWithCheck("isQuestion(6)");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);
		f=parseWithCheck("isQuestion(null)");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);

		f=parseWithCheck("min(2,3)==2");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);
		f=parseWithCheck("min(2,3,5,null,1,-2)==-2");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);
		f=parseWithCheck("min(2,3)==4");
		r=evaluate(mykb,f);
		assertFalse((Boolean)r);
		f=parseWithCheck("min(2,null)==2");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);
		f=parseWithCheck("min(null,null)==null");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		f=parseWithCheck("+(2,if(true,3,4))==5");
		r=evaluate(mykb,f);
		assertTrue((Boolean)r);

		e=DialogueOperatorEffect.createAssignment(parseWithCheck("e"),new Double(1));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("e");
		r=evaluate(mykb,f);
		assertTrue(r instanceof Number && r.equals(1l));
		e=DialogueOperatorEffect.createAssignment(parseWithCheck("e"),new HashMap<String,String>());
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		f=parseWithCheck("e");
		r=evaluate(mykb,f);
		assertTrue(r instanceof HashMap);

		e=DialogueOperatorEffect.createAssignment(parseWithCheck("e"),"'test'");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		Object v=mykb.get("e");
		assertTrue((v!=null) && (v instanceof DialogueKBFormula) && ((DialogueKBFormula) v).isString());
		assertTrue(v.equals("'test'"));

		e=DialogueOperatorEffect.parse("assign(e,'test')");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		v=mykb.get("e");
		assertTrue((v!=null) && (v instanceof DialogueKBFormula) && ((DialogueKBFormula) v).isString());
		assertTrue(v.equals("'test'"));

		e=DialogueOperatorEffect.parse("assign(f,'test')");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.parse("assign(e,f)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		v=mykb.get("e");
		assertTrue((v!=null) && (v instanceof DialogueKBFormula) && ((DialogueKBFormula) v).isString());
		assertTrue(v.equals("'test'"));

		e=DialogueOperatorEffect.parse("assign(e,'test')");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		v=mykb.get("e");
		assertTrue((v!=null) && (v instanceof DialogueKBFormula) && ((DialogueKBFormula) v).isString());
		assertTrue(v.equals("'test'"));

		e=DialogueOperatorEffect.parse("imply(e==2,e=3,e=4)");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.parse("e=2");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		v=mykb.get("e");
		assertTrue(v.equals(3l));
		e=DialogueOperatorEffect.parse("e=5");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		v=mykb.get("e");
		assertTrue(v.equals(4l));

		mykb.clearKBTree();
		e=DialogueOperatorEffect.createImplication(DialogueKBFormula.parse("e==2"), DialogueOperatorEffect.parse("e=3"), null);
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		e=DialogueOperatorEffect.parse("e=2");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		v=mykb.get("e");
		assertTrue(v.equals(3l));
		e=DialogueOperatorEffect.parse("e=5");
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		v=mykb.get("e");
		assertTrue(v.equals(5l));

		mykb.clearKBTree();
		boolean rr=false;
		try {
			e=DialogueOperatorEffect.createImplication(DialogueKBFormula.parse("e==2"), null,DialogueOperatorEffect.parse("e=4"));
		} catch (Exception e1) {rr=true;}
		assertTrue(rr);
	}
	// basic formula matching
	public void test7() throws Exception {
		DialogueOperatorEffect e1=DialogueOperatorEffect.createAssignment(parseWithCheck("a"),2);
		DialogueOperatorEffect e2=DialogueOperatorEffect.createAssignment(parseWithCheck("a"),2);
		DialogueKBFormula f1 = parseWithCheck("not(a)");
		DialogueKBFormula f2 = parseWithCheck("~a");
		assertTrue(f1==f2);
		f1 = parseWithCheck("not(a)");
		f2 = parseWithCheck("!a");
		assertTrue(f1==f2);
		assertTrue(e1.equals(e2));
		f1=parseWithCheck("+(a,2)");
		f2=parseWithCheck("+(a,2.000)");
		assertTrue(f1==f2);
		f1=parseWithCheck("+(a,2)");
		f2=parseWithCheck("add(a,2.000)");
		assertTrue(f1==f2);
		f1=parseWithCheck("+(a,2)");
		f2=parseWithCheck("ADD(a,2.000)");
		assertTrue(f1==f2);
	}
	
	public void test8() throws Exception {
		DialogueKBFormula f1 = parseWithCheck("+(2,3)");
		DialogueKBFormula f2 = parseWithCheck("5");
		assertTrue(f1==f2);
		f1 = parseWithCheck("and(true,not(false))");
		f2 = parseWithCheck("true");
		assertTrue(f1==f2);
		f1 = parseWithCheck("+(+(a,1),3)");
		f2 = parseWithCheck("+(a,4)");
		assertTrue(f1==f2);
		f1 = parseWithCheck("+(3,+(a,1))");
		f2 = parseWithCheck("+(a,4)");
		assertTrue(f1==f2);
		f1 = parseWithCheck("+(3,+(+(b,3),1))");
		f2 = parseWithCheck("+(b,7)");
		assertTrue(f1==f2);
		f1 = parseWithCheck("+(3,-(b,3))");
		f2 = parseWithCheck("+(b,0)");
		assertTrue(f1==f2);
		f1 = parseWithCheck("-(-(b,3),3)");
		f2 = parseWithCheck("+(b,-6)");
		assertTrue(f1==f2);
		/*f1 = parseWithCheck("+(b,-3)");
		f2 = parseWithCheck("-(b,3)");
		assertTrue(f1==f2);*/
	}
	// evaluation of effects
	public void test9() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		Boolean r=mykb.evaluate(e);
		assertTrue(r==null);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null).negate());
		r=mykb.evaluate(e);
		assertTrue(r==null);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		r=mykb.evaluate(e);
		assertTrue(r);
		e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		r=mykb.evaluate(e);
		assertTrue(!r);

		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),null);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),2);
		r=mykb.evaluate(e);
		assertTrue(!r);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),null);
		r=mykb.evaluate(e);
		assertTrue(r);
		
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),3);
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),2);
		r=mykb.evaluate(e);
		assertTrue(!r);
		e=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("test", null),3);
		r=mykb.evaluate(e);
		assertTrue(r);
	}
	//quotation
	public void test10() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e1=DialogueOperatorEffect.createAssignment(parseWithCheck("timeSinceLast"),2);
		DialogueOperatorEffect e2 = DialogueOperatorEffect.parse("secondsSinceLast=quote(timeSinceLast)");
		mykb.store(e1, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
		mykb.store(e2, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
		DialogueKBFormula f = parseWithCheck("secondsSinceLast");
		Float r = FloatAndLongUtils.numberToFloat((Number) evaluate(mykb,f));
		assertTrue(r==2.0f);
	}
	
	public void test11() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		TrivialDialogueKB mykb2 = new TrivialDialogueKB(mykb);
		TrivialDialogueKB mykb3 = new TrivialDialogueKB(mykb2);

		DialogueOperatorEffect e1=DialogueOperatorEffect.createAssignment(parseWithCheck("a"),2);
		mykb.store(e1, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		DialogueKBFormula f = parseWithCheck("a");
		Object r=evaluate(mykb3,f);
		assertTrue((Long)r==2);
		mykb3.removeVariable("a", ACCESSTYPE.AUTO_OVERWRITEAUTO);
		r=evaluate(mykb3,f);
		assertTrue(r==null);
	}
	
	/**
	 * tests forward inference
	 * @throws Exception
	 */
	public void test12() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		TrivialDialogueKB mykb2 = new TrivialDialogueKB(mykb);
		DialogueOperatorEffect e=DialogueOperatorEffect.parse("imply(and(doneQuestion,!known(forcedstate)),assignlist(assign(forcedstate,'howareyou'),assign(doneQuestion,false)))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.parse("imply(and(doneQuestion,forcedstate=='howareyou'),assignlist(assign(forcedstate,'la'),assign(doneQuestion,false)))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		mykb.setValueOfVariable("doneQuestion", false, ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb.setValueOfVariable("forcedstate", null, ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb2.setValueOfVariable("doneQuestion", true, ACCESSTYPE.AUTO_OVERWRITETHIS);
		mykb.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
		DialogueKBFormula f = parseWithCheck("forcedState");
		Object r=evaluate(mykb,f);
		assertTrue(r==null);
		f = parseWithCheck("doneQuestion");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);
		f = parseWithCheck("doneQuestion");
		r=evaluate(mykb2,f);
		assertTrue((Boolean)r);
		
		mykb2.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
		f = parseWithCheck("forcedState");
		r=evaluate(mykb,f);
		assertTrue(r==null);
		r=evaluate(mykb2,f);
		assertTrue(r.equals("'howareyou'"));
		f = parseWithCheck("doneQuestion");
		r=evaluate(mykb,f);
		assertTrue(!(Boolean)r);
		f = parseWithCheck("doneQuestion");
		r=evaluate(mykb2,f);
		assertTrue(!(Boolean)r);

		mykb.setValueOfVariable("doneQuestion", true, ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
		f = parseWithCheck("forcedState");
		r=evaluate(mykb,f);
		assertTrue(r.equals("'howareyou'"));
		r=evaluate(mykb2,f);
		assertTrue(r.equals("'howareyou'"));
		f = parseWithCheck("doneQuestion");
		r=evaluate(mykb,f);
		assertTrue(r.equals(false));

		mykb.setValueOfVariable("doneQuestion", true, ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb2.removeVariable("doneQuestion", ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb2.removeVariable("forcedstate", ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb.setValueOfVariable("forcedstate", null, ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb2.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
		f = parseWithCheck("forcedState");
		r=evaluate(mykb,f);
		assertTrue(r.equals("'howareyou'"));
		r=evaluate(mykb2,f);
		assertTrue(r.equals("'howareyou'"));
		f = parseWithCheck("doneQuestion");
		r=evaluate(mykb,f);
		assertTrue(r.equals(false));
	}

	public void test13() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.parse("imply(a,assign(b,false))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		e=DialogueOperatorEffect.parse("imply(a,assign(b,true))");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		mykb.setValueOfVariable("a", true, ACCESSTYPE.AUTO_OVERWRITEAUTO);
		mykb.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
		DialogueKBFormula f = parseWithCheck("b");
		Object r=evaluate(mykb,f);
		assertTrue((Boolean)r);
	}

	public void test15() throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e=DialogueOperatorEffect.parse("a=currenttime()");
		mykb.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		DialogueKBFormula f = parseWithCheck("a");
		Object r=evaluate(mykb,f);
		System.out.println(r);
	}
	
	private DialogueKBFormula parseWithCheck(String fs) throws Exception {
		DialogueKBFormula f=DialogueKBFormula.parse(fs);
		assertTrue(f==DialogueKBFormula.parse(f.toString()));
		return f;
	}
	public static Test suite(){
		return new TestSuite(TrivialKBTest.class);
	}
	
	public static void main(String[] args) throws Exception {
		TrivialKBTest t = new TrivialKBTest();
		t.test15();
		System.exit(0);
	}
}