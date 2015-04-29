package edu.usc.ict.nl.dm.reward;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.Node;

public class StateTracker {
	// 1->2->2->3->2->3->3->4
	//       s  e  b  ce
	private class LoopMarker {
		public LoopMarker(Node start, Node end, Node linkBack,Node currentEnd) throws Exception {
			this.start=start;
			this.startDepth=getDepthOfnode(this.start);
			this.end=end;
			this.startLinkBackToStart=linkBack;
			this.currentEnd=currentEnd;
			this.loops=0;
			this.endPeriodDepth=getDepthOfnode(this.startLinkBackToStart);
			this.period=endPeriodDepth-startDepth;
		}
		int period,startDepth,endPeriodDepth;
		Node start;
		Node end;
		Node startLinkBackToStart;
		int loops;
		Node currentEnd;
		
		@Override
		public String toString() {
			try {
				return "<"+start.getName()+" loops: "+loops+" ["+getDepthOfnode(start)+","+getDepthOfnode(startLinkBackToStart)+","+getDepthOfnode(currentEnd)+"]>";
			} catch (Exception e) {
				return null;
			}
		}

		public boolean isSuperflous(LoopMarker nl) {
			if (((nl.period==period) || ((nl.period%period)==0)) && nl.startDepth>=startDepth && (nl.startDepth<=(startDepth+period))) return true;
			return false;
		}
	}
	private List<LoopMarker> possibleLoops=new ArrayList<StateTracker.LoopMarker>();

	public int getDepthOfnode(Node n) throws Exception {
		int c=0;
		while(n!=null) {
			n=n.getSingleParent();
			c++;
		}
		return c;
	}
	
	private final Node root=new Node("root");
	private Node currentNode=root;
	public class UserInput extends Edge {
		private String sa;
		public UserInput(String sa) {
			this.sa=sa;
		}
	}

	public void addState(String userInput, String resultingActiveState) throws Exception {
		UserInput edge = new UserInput(userInput);
		edge.setSource(currentNode);
		Node target=new Node(resultingActiveState);
		edge.setTarget(target);
		currentNode.addEdge(edge, false, false);
		
		updateLoopTracker(currentNode,edge);
		
		
		currentNode=target;
	}

	private void updateLoopTracker(Node startNode, UserInput target) throws Exception {
		if (possibleLoops!=null) {
			Iterator<LoopMarker> it=possibleLoops.iterator();
			while(it.hasNext()) {
				LoopMarker lm=it.next();
				assert(lm.currentEnd==startNode);
				if (hasSimilarChildLink(lm.end,target)) {
					// elongate recognized possible loop.
					// if target of end is startLinkBackToStart then increment loop counter and set end to start
					// update current end to target.
					Node nextEnd=lm.end.getFirstChild();
					if (nextEnd==lm.startLinkBackToStart) {
						lm.loops++;
						lm.end=lm.start;
					} else {
						lm.end=nextEnd;
					}
					lm.currentEnd=target.getTarget();
				} else {
					// kill this possible loop because it cannot be elongated anymore.
					it.remove();
				}
			}
		}
		// search for new possible loops to be started
		addAllNewPossibleLoops(startNode, target.getTarget());
	}

	private boolean hasSimilarChildLink(Node startNode, UserInput edge) {
		Collection<Edge> edges = startNode.getOutgoingEdges();
		assert(edges==null || edges.isEmpty() || (edges.size()==1));
		if (edges!=null && !edges.isEmpty()) {
			UserInput availEdge=(UserInput) edges.iterator().next();
			String childNodeName=availEdge.getTarget().getName();
			String inputNodeName=edge.getTarget().getName();
			if (availEdge.sa.equals(edge.sa) &&
					((childNodeName!=null && childNodeName.equals(inputNodeName)) ||
							childNodeName==inputNodeName))
				return true;
		}
		return false;
	}

	private void addAllNewPossibleLoops(Node startNode, Node target) throws Exception {
		if (target!=null) {
			String targetName=target.getName();
			while(startNode!=null && startNode!=root) {
				String startName=startNode.getName();
				if ((startName!=null && startName.equals(targetName)) || startName==targetName) {
					LoopMarker nl=new LoopMarker(startNode,startNode,target,target);
					if (!thereIsAPossibleLoopSubsumingThis(nl))
						possibleLoops.add(nl);
				}
				startNode=startNode.getSingleParent();
			}
		}
	}
	
	private boolean thereIsAPossibleLoopSubsumingThis(LoopMarker nl) {
		if (!possibleLoops.isEmpty()) {
			for(LoopMarker el:possibleLoops) {
				if (el.isSuperflous(nl)) return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		List<Integer> positionOfNodes=new ArrayList<Integer>();
		Node cn=root;
		StringBuffer b=new StringBuffer();
		while(cn!=null) {
			positionOfNodes.add(b.length());
			b.append(cn.getName());
			List<Edge> edges = cn.getOutgoingEdges();
			if (edges!=null && !edges.isEmpty()) {
				UserInput e=(UserInput)edges.get(0);
				//b.append("-"+e.sa+"->");
				b.append("->");
				cn=e.getTarget();
			} else break;
		}
		if (possibleLoops!=null && !possibleLoops.isEmpty()) {
			b.append("\n");
			int l=b.length();
			Iterator<LoopMarker> it=possibleLoops.iterator();
			while(it.hasNext()) {
				LoopMarker lm=it.next();
				int f=positionOfNodes.get(lm.startDepth-1),s=positionOfNodes.get(lm.endPeriodDepth-1);
				for(int i=0;i<l;i++) {
					if (i<=s && i>=f)
						if (i==f)
							b.append("\\");
						else if (i==s)
							b.append("/");
						else
							b.append("_");
					else
						b.append(" ");
				}
				if (it.hasNext()) b.append("\n");
			}
		}
		return b.toString();
	}
	
	public int containsLoop() {
		int max=-1;
		if (possibleLoops!=null) {
			for(LoopMarker lm:possibleLoops) {
				if (lm.loops>0 && lm.loops>max) max=lm.loops; 
			}
		}
		return max;
	}

	public boolean isSameLastState(String state) {
		return (currentNode!=null && ((currentNode.getName()!=null && currentNode.getName().equals(state)) || currentNode.getName()==state));
	}

	public static void main(String[] args) throws Exception {
		StateTracker st = new StateTracker();
		st.addState("a", "1");
		st.addState("a", "2");
		st.addState("a", "2");
		st.addState("a", "3");
		st.addState("a", "2");
		st.addState("a", "4");
		st.addState("a", "2");
		st.addState("a", "3");
		st.addState("a", "2");
		System.out.println(st);
		st.addState("a", "4");
		System.out.println(st);
	}
}
