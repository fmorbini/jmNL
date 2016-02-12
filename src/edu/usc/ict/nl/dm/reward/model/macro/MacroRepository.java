package edu.usc.ict.nl.dm.reward.model.macro;

import java.util.HashMap;
import java.util.Map;

public class MacroRepository {
	private static Map<String,Macro> repo=new HashMap<>();
	
	public static void addMacro(String name,Macro macro) {
		repo.put(name, macro);
	}
	
	public static boolean containsMacro(String name) {
		return repo.containsKey(name);
	}
	
	public static Macro getMacro(String name) {
		return repo.get(name);
	}

	public boolean isEmpty() {
		return repo.isEmpty();
	}

}
