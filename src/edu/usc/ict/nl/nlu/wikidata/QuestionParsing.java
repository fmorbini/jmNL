package edu.usc.ict.nl.nlu.wikidata;

import java.util.List;

import com.clearnlp.dependency.DEPTree;

import edu.usc.ict.nl.nlu.clearnlp.CONLL;
import edu.usc.ict.nl.nlu.clearnlp.DepNLU;

public class QuestionParsing {
	
	public static void main(String[] args) throws Exception {
		DepNLU parser=new DepNLU();
		List<DEPTree> result = parser.parse("tell me the capital of france?", System.out);
		
		System.out.println(parser.enrichedInputString(result,"_"));
		int id=1;
		if (result!=null) {
			for(DEPTree r:result) {
				new CONLL(r).toGDLGraph("sentence-"+id+".gdl");
				id++;
			}
		}
		

	}
}
