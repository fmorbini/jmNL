package edu.usc.ict.nl.nlg.lf.pos;

import java.util.LinkedList;
import java.util.List;

public abstract class POS implements POSI {
	@Override
	public void put(List<Integer> coord, POS el) {
		if (coord==null || coord.isEmpty()) System.err.println("cannot put at null coord.");
		else {
			POS parent=this;
			int i=0;
			for(;i<coord.size()-1;i++) {
				parent=parent.getChildren().get(coord.get(i));
			}
			parent.updateChild(coord.get(i), el);
		}
	}
	
	public POS getElementAt(List<Integer> coord) {
		if (coord==null || coord.isEmpty()) return this;
		else {
			POS parent=this;
			int i=0;
			for(;i<coord.size()-1;i++) {
				parent=parent.getChildren().get(coord.get(i));
			}
			return parent.getChildren().get(coord.get(i));
		}
	}

	@Override
	public POS clone() {
		return null;
	}
	
	@Override
	public void updateChild(int pos, POS child) {
		getChildren().set(pos, child);
	}

	/**
	 * given a coord that identifies a node, (null or empty identifies the root node) return the next node to be visited for
	 * a depth first traversal of the tree ruted at this.
	 * @param coord
	 * @return
	 */
	public POS getNextItem(LinkedList<Integer> coord) {
		//first check if there are children to the currently selected node.
		POS current=getElementAt(coord);
		List<POS> cs=current.getChildren();
		if (cs!=null && !cs.isEmpty()) {
			for(int i=0;i<cs.size();i++) {
				POS child=cs.get(i);
				if (child!=null) {
					coord.addLast(i);
					return child;
				}
			}
		}
		do {
			// if not, then backtrack until you find the next available node to visit.
			int last=(coord.isEmpty())?-1:coord.removeLast();
			POS parent=getElementAt(coord);
			cs = parent.getChildren();
			if (cs!=null && cs.size()>last+1) {
				for(int lasti=last+1;lasti<cs.size();lasti++) {
					POS child=cs.get(last+1);
					if (child!=null) {
						coord.addLast(last+1);
						return child;
					}
				}
			}
		} while (!coord.isEmpty());
		return null;
	}

}
