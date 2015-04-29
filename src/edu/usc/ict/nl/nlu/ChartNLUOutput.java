package edu.usc.ict.nl.nlu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.usc.ict.nl.nlu.chart.PartialClassification;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.Triple;

public class ChartNLUOutput extends NLUOutput {

	private List<Triple<Integer,Integer,NLUOutput>> portions=null;
	
	public ChartNLUOutput(String text,Collection<PartialClassification> portions) throws Exception {
		super(text,"",1f,null);
		if (portions!=null) {
			for(PartialClassification pc:portions) {
				addPortion(pc.getStart(), pc.getEnd(), pc.getTopResult());
			}
		}
	}
	public void addPortion(int start, int end, NLUOutput annotation) throws Exception {
		boolean first=false;
		if (this.portions==null || this.portions.isEmpty()) {
			first=true;
			this.portions=new ArrayList<Triple<Integer,Integer,NLUOutput>>();
		}
		this.portions.add(new Triple<Integer, Integer, NLUOutput>(start, end, annotation));
		if (first) {
			this.setId(annotation.getId());
			this.setProb(annotation.getProb());
		} else {
			this.setId(this.getId()+"||"+annotation.getId());
			this.setProb(this.getProb().times(annotation.getProb()));
			this.setProb(new Rational(Math.pow(this.getProb().toDouble(),(double)1/(double)portions.size())));
		}
	}
	
	public List<Triple<Integer, Integer, NLUOutput>> getPortions() {return portions;}
	
	@Override
	public String toString() {
		String ret="<"+((getNluID()!=null)?getNluID()+" ":"")+getText()+"\n";
		if (portions!=null) {
			for(Triple<Integer,Integer,NLUOutput> pc:portions) {
				int start=pc.getFirst(),end=pc.getSecond();
				NLUOutput a=pc.getThird();
				ret+=" ["+start+","+end+"] "+a+"\n";
			}
		}
		ret+=">";
		return ret;
	}
	
	@Override
	public String getId() {
		String ret=null;
		if (portions!=null) {
			StringBuffer b=new StringBuffer();
			boolean isFirst=true;
			for (Triple<Integer,Integer,NLUOutput> pc:portions) {
				NLUOutput nlu=pc.getThird();
				if (nlu!=null) b.append(((isFirst)?"":"||")+nlu.getId());
				isFirst=false;
			}
			ret=b.toString(); 
		}
		return ret;
	}
	
	@Override
	public String toXml() {
		String ret="<nlu id=\"chart "+((portions!=null)?portions.size():"0")+"\" prob=\""+getProb().getResult()+"\"/>\n";
		if (portions!=null) {
			int size=portions.size(),k=1;
			for (Triple<Integer,Integer,NLUOutput> pc:portions) {
				int start=pc.getFirst(),end=pc.getSecond();
				NLUOutput a=pc.getThird();
				ret+="<nlu id=\""+(k++)+"/"+size+" ["+start+","+end+"] "+a.getId()+"\" prob=\""+a.getProb().getResult()+"\" payload=\""+((a.getPayload()==null)?null:a.getPayload())+"\"/>\n";
			}
		}
		return ret;
	}

	@Override
	public boolean matchesInterpretation(String id,String nluID) {
		List<Triple<Integer, Integer, NLUOutput>> prts = getPortions();
		if (prts==null || prts.isEmpty()) {
			return super.matchesInterpretation(id,nluID);
		} else {
			for(Triple<Integer, Integer, NLUOutput> part:prts) {
				if (part.getThird().matchesInterpretation(id,nluID)) return true;
			}
			return false;
		}
	}
	@Override
	public boolean matchesInterpretation(String id) {
		return matchesInterpretation(id, null);
	}
}
