package edu.usc.ict.nl.nlu;

import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;

public class TrainingDataFormat {
	// extra fields of information
	public static final String FILE = "file";
	
	protected String utt;
	private String label;
	private String features;
	private String[] backupLabels=null;
	private String id;
	private Map<String,String> extra=null;
	
	public TrainingDataFormat() {}
	public TrainingDataFormat(TrainingDataFormat src) {
		utt=src.utt;
		label=src.label;
		id=src.getId();
		if (src.backupLabels!=null) backupLabels=src.backupLabels.clone();
	}
	public TrainingDataFormat(String u,String l) throws Exception {
		this(u,l,false);
	}
	public TrainingDataFormat(String u,String l,boolean noEmptyLabelCheck) throws Exception {
		if (StringUtils.isEmptyString(u)) throw new Exception("empty utterance.");
		if (!noEmptyLabelCheck && StringUtils.isEmptyString(l)) throw new Exception("empty label.");
		this.utt=u;
		this.label=l;
	}
	public void setBackupLabels(String[] labels) {
		if (labels!=null) {
			backupLabels=labels;
		}
	}
	
	public void addExtraInfo(String key,String value) {
		if (extra==null) extra=new HashMap<String, String>();
		extra.put(key, value);
	}
	
	public Map<String, String> getExtraInfo() {
		return extra;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String toNluformat(NLU nlu) {
		String fs=null;
		try {
			fs = FunctionalLibrary.printCollection(nlu.getFeaturesFromUtterance(getUtterance()), "", "", " ");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!StringUtils.isEmptyString(fs)) return "<s> "+utt+" </s>\n"+fs+"\n\n"+label+"\n\n";
		else return "<s> "+utt+" </s>\n\n"+label+"\n\n";
	}

	public String getUtterance() {return utt;}
	public String getLabel() {return label;}
	public String[] getBackupLabels() {return backupLabels;}
	public boolean hasBackupLabels() {return (backupLabels!=null);}
	public void setUtterance(String u) {this.utt=u;}
	public void setLabel(String l) {this.label=l;}
	public void setFeatures(String features) {this.features=features;}
	public String getFeatures() {return this.features;}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TrainingDataFormat) {
			TrainingDataFormat ot=(TrainingDataFormat)o;
			if (o==null) return false;
			else {
				return getUtterance().equals(ot.getUtterance()) && getLabel().equals(ot.getLabel());
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getUtterance()+": "+getLabel();
	}
	
	public boolean match(String id) {
		if (label.equals(id)) return true;
		else if (backupLabels!=null) {
			for (String bl:backupLabels) {
				if (bl.equals(id)) return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		TrainingDataFormat td=new TrainingDataFormat("t", "a");
		td.setBackupLabels(new String[]{"b","c"});
		System.out.println(td.match("a"));
		System.out.println(td.match("b"));
		System.out.println(td.match("c"));
		System.out.println(td.match("d"));
	}
}
