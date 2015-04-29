package edu.usc.ict.nl.nlu.fst.test;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.usc.ict.nl.nlu.fst.FSTNLUOutput;

public class FSTTests {

	@Test
	public void testFSTdecomposeNLUOutput() {
		String test = "type:yesno type:pattern what:fever";
		List<Map<String,String>> maps = FSTNLUOutput.decomposeNLUOutput(test);
		assertTrue(maps.size() == 2);
		int count=0;
		for (Map<String,String> map:maps) {
			if (map.containsKey("type"))
				count++;
			
		}
		assertTrue(count==2);
		
		test = "type:yesno what:fever";
		maps = FSTNLUOutput.decomposeNLUOutput(test);
		assertTrue(maps.size() == 1);
		count=0;
		for (Map<String,String> map:maps) {
			if (map.containsKey("type"))
				count++;
			
		}
		assertTrue(count==1);
		
		
		
		test = "a:1 b:4 c:6 a:2 b:5 c:7 c:8";
		maps = FSTNLUOutput.decomposeNLUOutput(test);
		assertTrue(maps.size() == 12);
		count=0;
		for (Map<String,String> map:maps) {
			if (map.containsKey("a"))
				count++;
			
		}
		assertTrue(count==12);
		

		test = "a:1 a:2 b:4 b:5 c:6 c:7 c:8 c:9";
		maps = FSTNLUOutput.decomposeNLUOutput(test);
		assertTrue(maps.size() == 16);
		count=0;
		for (Map<String,String> map:maps) {
			if (map.containsKey("c"))
				count++;
			
		}
		assertTrue(count==16);
		
	}

}
