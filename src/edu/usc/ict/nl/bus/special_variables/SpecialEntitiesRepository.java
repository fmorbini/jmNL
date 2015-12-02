package edu.usc.ict.nl.bus.special_variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import edu.usc.ict.nl.config.NLBusConfig;

public class SpecialEntitiesRepository {
	private final LinkedHashMap<String, SpecialVar> svs=new LinkedHashMap<String, SpecialVar>();
	private NLBusConfig config;
	
	public SpecialEntitiesRepository(NLBusConfig configuration) {
		this.config=configuration;
	}
	public void addSpecialVariable(SpecialVar sv) {
		String name=sv.getName();
		if (config==null || !config.dmConfig.getCaseSensitive()) name=name.toLowerCase();
		svs.put(name, sv);
	}
	public SpecialVar get(String name) {
		if (config==null || !config.dmConfig.getCaseSensitive()) name=name.toLowerCase();
		return svs.get(name);
	}
	public Collection<SpecialVar> getVisibleVars() {
		Collection<SpecialVar> ret=null;
		for(SpecialVar sv:svs.values()) {
			if (!sv.isHidden()) {
				if (ret==null) ret=new ArrayList<SpecialVar>();
				ret.add(sv);
			}
		}
		return ret;
	}
	public List<SpecialVar> getAllSpecialVariables() {
		return (svs!=null)?new ArrayList<SpecialVar>(svs.values()):null;
	}
}
