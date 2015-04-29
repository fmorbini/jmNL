package edu.usc.ict.nl.dm.reward.possibilityGraph;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.dm.reward.DormantActions;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.SwapoutReason;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodesChain;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;

public class PossibleIS extends OperatorHistoryNode {
	
	/* the value is the information state at the end of this possible dialogue state.
	 * the chain reward is the reward gotten to reach that state from the parent state.
	 */
	
	public enum Type {ROOT,CHAIN,EC};
	private Type type;
	public Type getType() {return type;}
	public void setType(Type type) {this.type=type;}
	public boolean isRoot() {return getType()==PossibleIS.Type.ROOT;}
	public boolean isEntranceCondition() {return getType()==PossibleIS.Type.EC;}
	public boolean isChain() {return getType()==PossibleIS.Type.CHAIN;}
	
	private float chainReward=0; // reward of this node (if it;s a chain)
	private Float treeReward=null; // reward of the tree rooted at this node without considering the node itself
	private DormantActions dormantActions=null;
	private HashMap<DialogueOperator,Float> contributes=null;
	
	public boolean isTreeRewardSet() {return treeReward!=null;}
	public float getTreeReward() {return treeReward;}
	public void setTreeReward(float r) {this.treeReward=r;}

	public DormantActions getDormantOperators() {return dormantActions;}
	
	public float getChainReward() {return chainReward;}
	public void setChainReward(float r) {this.chainReward=r;}

	public HashMap<DialogueOperator, Float> getContributes() {return contributes;}
	public void setContributes(HashMap<DialogueOperator, Float> c) {contributes=c;}
	
	public void addDormantAction(DialogueAction a) {
		if (a!=null) {
			DialogueOperator op=a.getOperator();
			if (dormantActions!=null && !dormantActions.isThisDormant(op)) {
				dormantActions=new DormantActions(dormantActions);
				dormantActions.addAction(a,SwapoutReason.SEARCH);
			}
		}
	}
	public void removeDormantAction(DialogueOperator op) {
		if (op!=null) {
			if (dormantActions!=null && dormantActions.isThisDormant(op)) {
				dormantActions=new DormantActions(dormantActions);
				dormantActions.removeDormantOperator(op,true);
			}
		}
	}
	
	private HashMap<String,PossibleIS> createdNodes=null;
	public HashMap<String,PossibleIS> getContentCheckDB() {return createdNodes;}
	public void setContentCheckDB(HashMap<String,PossibleIS> db) {this.createdNodes=db;}
	public void resetContentCheck() {
		createdNodes.clear();
	}
	public boolean tryToMergeStopIfLoop() throws Exception {
		String id=getContentID(getValue(), dormantActions);
		PossibleIS n=createdNodes.get(id);
		if (n==null) {
			createdNodes.put(id, this);
			return false;
		} else {
			addEdgeTo(n, null, null);
			return true;
		}
	}
	public void addToRecord() throws Exception {
		String id=getContentID(getValue(), dormantActions);
		createdNodes.put(id, this);
	}

	public PossibleIS(DialogueOperator op,Type type,String name,DialogueKB is,DormantActions dormantActions) {
		super(op,true);
		setName(type.toString()+" "+name);
		setType(type);
		assert(is!=null);
		setValue(is);
		this.dormantActions=dormantActions;
	}

	public boolean isDormant(DialogueOperator op) {return ((dormantActions!=null) && dormantActions.isThisDormant(op));}
	
	public static Pair<DialogueKB,Float> updateISAndgetReward(DialogueKB is, DialogueOperatorNodesChain pathAndEffects) throws Exception {
		float chainReward=0;

		DialogueKB retIS = is;
		boolean alreadyCreatedAnewKB=false;
		if (pathAndEffects!=null && !pathAndEffects.isEmpty()) {
			for(Triple<GraphElement, Rational, List<DialogueOperatorEffect>> chainEl:pathAndEffects.getChain()) {
				List<DialogueOperatorEffect> effects=chainEl.getThird();

				//float probability=chainEl.getSecond();
				if (effects!=null && !effects.isEmpty()) {
					List<DialogueOperatorEffect> isEffects=null;
					for(DialogueOperatorEffect eff:effects) {
						if (eff.isGoalAchievement()) {
							float reward = eff.evaluateGoalValueIn(retIS);
							chainReward+=reward;//*probability;
						} else {
							if (isEffects==null) isEffects=new ArrayList<DialogueOperatorEffect>();
							isEffects.add(eff);
						}
					}
					DialogueKB newIS = retIS.storeAll(isEffects,alreadyCreatedAnewKB?ACCESSTYPE.AUTO_OVERWRITETHIS:ACCESSTYPE.AUTO_NEW,true);
					assert((newIS==null) || (newIS!=retIS) || alreadyCreatedAnewKB);
					if (newIS!=null) {
						retIS=newIS;
						alreadyCreatedAnewKB=true;
					}
				}
			}
			//chainReward*=pathAndEffects.getWeight();

			DialogueOperatorEffect eff = RewardDM.updateLastNonNullOperatorVariableInWith(is, pathAndEffects.getOperator());
			if (eff!=null) {
				DialogueKB newIS = retIS.store(eff, alreadyCreatedAnewKB?ACCESSTYPE.AUTO_OVERWRITETHIS:ACCESSTYPE.AUTO_NEW, true);
				assert((newIS==null) || (newIS!=retIS) || alreadyCreatedAnewKB);
				if (newIS!=null) {
					retIS=newIS;
				}
			}
		}
		return new Pair<DialogueKB,Float>(retIS,chainReward);
	}

	/** execute the effects in order,
	* update IS and get the appropriate reward for goal achieving effects.
	*/
	public PossibleIS simulateExecutionOfChain(DialogueOperatorEntranceTransition ec, DialogueOperatorNodesChain chain) throws Exception {
		DialogueKB is=getValue();
		
		Pair<DialogueKB, Float> is_reward = updateISAndgetReward(is,chain);
		PossibleIS targetIS=new PossibleIS(chain.getOperator(),PossibleIS.Type.CHAIN, (chain!=null)?chain.toString():"", is_reward.getFirst(), dormantActions);
		targetIS.removeDormantAction(ec.getOperator());
		targetIS.setChainReward(is_reward.getSecond());
		targetIS.setWeight(chain.getRationalWeight());
		assert(targetIS.getWeight()>0);
		
		addEdgeTo(targetIS,ec,chain);
		return targetIS;
	}

	public boolean addEdgeTo(PossibleIS n, DialogueOperatorEntranceTransition ec, DialogueOperatorNodesChain chain) throws Exception {
		if (n!=null) {
			n.setContentCheckDB(getContentCheckDB());
			Edge edge=new PossibleTransition(ec,chain);
			edge.setSource(this);
			edge.setTarget(n);
			return addEdge(edge,true,false);
		}
		return false;
	}
	
	public String prettyPrintKB() {
		try {
			String ret="";
			LinkedHashMap<String, Collection<DialogueOperatorEffect>> dump = getValue().dumpKBTree();
			if (dump!=null && !dump.isEmpty()) {
				for(String kbName:dump.keySet()) {
					ret+=kbName+":\n";
					for(DialogueOperatorEffect e:dump.get(kbName)) {
						ret+="  "+e.getID()+": "+e+"\n";
					}
				}
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getContentID(DialogueKBInterface is,DormantActions dormantActions) throws Exception {
		return is.getContentID()+":"+dormantActions;
	}
	
	@Override
	public String toString() {
		return getName()+" {"+getChainReward()+"}";
	}
	@Override
	public String gdlText() {
		try {
			//return "node: { color: "+getGDLColor()+" title: \""+getID()+"\" label: \""+toString()+"\" info1: \""+prettyPrintKB()+"\" info2: \""+dormantActions+"\" info3:\""+getValue().getContentID()+"\"}\n";
			return "node: { color: "+getGDLColor()+" title: \""+getID()+"\" label: \""+toString()+"\" info1: \"\" info2: \""+dormantActions+"\" info3:\""+getValue().getContentID()+"\"}\n";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "node: { color: "+getGDLColor()+" title: \""+getID()+"\" label: \""+toString()+"\" info1: \"error while printing\"}\n";
	}
	public String getGDLColor() {
		switch (getType()) {
		case ROOT: return "lightred";
		case CHAIN: return "lightblue";
		case EC: return "lightgreen";
		default: return "red";
		}
	}
	
	@Override
	public DialogueKB getValue() {
		return (DialogueKB) super.getValue();
	}
	public PossibleTransition getSingleIncomingTransition() throws Exception {
		List<Edge> ins = getIncomingEdges();
		if (ins!=null) {
			if (ins.size()>1) throw new Exception("more than one incoming transition: "+this);
			else return (PossibleTransition)ins.get(0);
		} else return null;
	}
	public String reason() {
		StringBuffer out=new StringBuffer();
		final HashMap<DialogueOperator, Float> cs = getContributes();
		if (cs!=null && !cs.isEmpty()) {
			ArrayList<DialogueOperator> ops = new ArrayList<DialogueOperator>(cs.keySet());
			Collections.sort(ops, new Comparator<DialogueOperator>() {
			    public int compare(DialogueOperator o1,DialogueOperator o2) {
			    	Float v1=cs.get(o1);
			    	Float v2=cs.get(o2);
			        if (v1==null || v2==null || v1==v2) return 0;
			        else if (v1<v2) return -1;
			        else return 1;
			    }});
			float total=0;
			for(DialogueOperator op:ops) {
				Float v=cs.get(op);
				if (v!=null) total+=Math.abs(v);
			}
			for(DialogueOperator op:ops) {
				Float v=cs.get(op);
				DecimalFormat df = new DecimalFormat("#.##");
				String percentage=df.format(v/total);
				if (v!=null) out.append("   >> "+op.getName()+"("+percentage+"): "+v+"\n");
			}
		}
		return out.toString();
	}
	public void log(Logger logger) {
		logger.debug(reason());
	}
	
	public EvalContext getEvalContextFromThis() {
		EvalContext context=new EvalContext(getValue(), getOperator());
		context.setExecutedOperatorsHistory(this);
		return context;
	}
}
