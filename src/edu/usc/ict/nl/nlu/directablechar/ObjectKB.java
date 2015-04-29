package edu.usc.ict.nl.nlu.directablechar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;


public class ObjectKB {
	private Map<COLOR,Set<DCObject>> color2Objects=null;
	private Map<SIZE,Set<DCObject>> size2Objects=null;
	private Map<SHAPE,Set<DCObject>> shape2Objects=null;
	private Map<String,Set<DCObject>> name2Objects=null;
	public static enum SIZE {BIG,SMALL,MEDIUM}
	public static enum SHAPE {SPHERE,BOX,CAPSULE};
	public static enum COLOR {RED,YELLOW,BLACK,GREEN,BLUE,PINK};
	static Method getSize;
	static Method getColor;
	static Method getShape;
	static {
		try {
			getSize=DCObject.class.getMethod("getSize");
			getColor=DCObject.class.getMethod("getColor");
			getShape=DCObject.class.getMethod("getShape");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ObjectKB() {
		color2Objects=new HashMap<ObjectKB.COLOR, Set<DCObject>>();
		size2Objects=new HashMap<ObjectKB.SIZE, Set<DCObject>>();
		shape2Objects=new HashMap<ObjectKB.SHAPE, Set<DCObject>>();
		name2Objects=new HashMap<String, Set<DCObject>>();
	}
	
	public void storeObject(DCObject obj) throws Exception {
		storeObjectIndexedByThisProperty(obj.getColor(),(Map)color2Objects,obj);
		storeObjectIndexedByThisProperty(obj.getSize(),(Map)size2Objects,obj);
		storeObjectIndexedByThisProperty(obj.getShape(),(Map)shape2Objects,obj);
		storeObjectIndexedByThisProperty(obj.getName(),(Map)name2Objects,obj);
	}

	public void removeObjectNamed(String objectName) throws Exception {
		DCObject obj=getObjectNamed(objectName);
		if (obj!=null) {
			removeObjectIndexedByThisProperty(obj.getName(),(Map)name2Objects,obj);
			removeObjectIndexedByThisProperty(obj.getShape(),(Map)shape2Objects,obj);
			removeObjectIndexedByThisProperty(obj.getSize(),(Map)size2Objects,obj);
			removeObjectIndexedByThisProperty(obj.getColor(),(Map)color2Objects,obj);
		}
	}

	public static String normalize(String in) {return (in!=null)?in.toLowerCase():in;}
	
	private void storeObjectIndexedByThisProperty(Object property,Map<Object, Set<DCObject>> store, DCObject obj) throws Exception {
		if (property!=null) {
			Set<DCObject> set=store.get(property);
			if (set==null) store.put(property, set=new HashSet<ObjectKB.DCObject>());
			set.add(obj);
		} else throw new Exception("null property for object "+obj);
	}
	private void removeObjectIndexedByThisProperty(Object property,Map<Object, Set<DCObject>> store, DCObject obj) throws Exception {
		if (property!=null) {
			Set<DCObject> set=store.get(property);
			if (set!=null) set.remove(obj);
		} else throw new Exception("null property for object "+obj);
	}

	public DCObject getObjectNamed(String name) {
		if (name2Objects!=null && !StringUtils.isEmptyString(name)) {
			name=normalize(name);
			Set<DCObject> obs=name2Objects.get(name);
			if (obs!=null && obs.size()==1) return obs.iterator().next();
		}
		return null;
	}
	
	public float[] getPositionForNewObject() {
		Random rand = new Random();
		return new float[]{(rand.nextFloat()-.5f)*10,(rand.nextFloat()-.5f)*10,0};
	}
	
	public DCObject completeProperties(SHAPE shape,SIZE size,COLOR color) throws Exception {
		DCObject ret=new DCObject(shape,size,color);
		if (shape!=null && size!=null && color!=null) return  ret;
		else {
			Set<DCObject> objectsWithTheseProperties=getObjectsWithProperties(shape,size,color);
			List<Class> nullConstraints=new ArrayList<Class>();
			if (shape==null) nullConstraints.add(SHAPE.class);
			if (size==null) nullConstraints.add(SIZE.class);
			if (color==null) nullConstraints.add(COLOR.class);
			boolean r=selectConstraint(0,nullConstraints,objectsWithTheseProperties,ret,false);
			if (!r) selectConstraint(0,nullConstraints,objectsWithTheseProperties,ret,true);
		}
		return ret;
	}

	private boolean selectConstraint(int ci,List<Class> allconstraints, Set<DCObject> objectsWithTheseProperties, DCObject ret,boolean forceSelection) throws Exception {
		Class c=allconstraints.get(ci);
		Method m=null;
		Class cl=null;
		if (c==SIZE.class) {
			m=getSize;
			cl=SIZE.class;
		} else if (c==SHAPE.class) {
			m=getShape;
			cl=SHAPE.class;
		} else if (c==COLOR.class) {
			m=getColor;
			cl=COLOR.class;
		} else {
			throw new Exception("invalid property type: "+c);
		}

		Set<Object> used=null;
		if (objectsWithTheseProperties!=null) {
			used=(Set) FunctionalLibrary.map(objectsWithTheseProperties, m);
		}
		Set<Object> all = new HashSet(EnumSet.allOf(cl));
		if (used!=null) all.removeAll(used);
		if (all.isEmpty()) {
			if (forceSelection)
				all = new HashSet(EnumSet.allOf(cl));
			else
				return false;
		}
		List pick=new ArrayList(all);
		Collections.shuffle(pick);
		int nextci=ci+1; 
		for (Object s:pick) {
			ret.setProperty(s);
			if (nextci>=allconstraints.size()) {
				return true;
			} else {
				boolean r=selectConstraint(nextci, allconstraints, objectsWithTheseProperties, ret,forceSelection);
				if (r) return true;
			}
		}
		return false;
	}

	private Set<DCObject> getObjectsWithProperties(SHAPE shape, SIZE size,COLOR color) {
		Set<DCObject> intersection=intersectWithConstraint(null, shape, shape2Objects);
		intersection=intersectWithConstraint(intersection, size, size2Objects);
		intersection=intersectWithConstraint(intersection, color, color2Objects);
		return intersection;
	}

	public <T> Set<DCObject> intersectWithConstraint(Set<DCObject> selectedObjects,T property,Map<T,Set<DCObject>> holder) {
		if (property!=null && holder!=null && 
				(selectedObjects==null || !selectedObjects.isEmpty())) {
			Set<DCObject> tmp = holder.get(property);
			if (tmp!=null) {
				if (selectedObjects==null) {
					selectedObjects=new HashSet<ObjectKB.DCObject>();
					selectedObjects.addAll(tmp);
				} else {
					selectedObjects.retainAll(tmp);
				}
			}
		}
		return selectedObjects;
	}
	
	public static float[] getSizeForShape(SHAPE shape, SIZE sizeMod) {
		switch (sizeMod) {
		case BIG:
			return new float[] {.4f,.4f,.4f};
		case SMALL:
			return new float[] {.1f,.1f,.1f};
		default:
			return new float[] {.2f,.2f,.2f};
		}
	}

	public class DCObject {
		SIZE size;
		SHAPE shape;
		COLOR color;
		String name;
		
		public DCObject() {
		}
		public DCObject(SHAPE sh,SIZE s,COLOR c) {
			this.size=s;
			this.shape=sh;
			this.color=c;
		}
		public void setProperty(Object p) {
			if (p instanceof SHAPE) {
				this.shape=(SHAPE) p;
			} else if (p instanceof SIZE) {
				this.size=(SIZE) p;
			} else if (p instanceof COLOR) {
				this.color=(COLOR) p;
			} else if (p instanceof String) {
				this.name=ObjectKB.normalize((String) p);
			}
		}
		public SHAPE getShape() {
			return shape;
		}
		public COLOR getColor() {
			return color;
		}
		public SIZE getSize() {
			return size;
		}
		public String getName() {
			return name;
		}
		
		@Override
		public String toString() {
			return "<"+name+": "+color+";"+shape+";"+size+">";
		}
		public String getShapeAsLiteral(String var) {
			return "(PROPERTY SHAPE "+getShape()+" "+var+")";
		}
		public String getSizeAsLiteral(String var) {
			return "(PROPERTY SIZE "+getSize()+" "+var+")";
		}
		public String getColorAsLiteral(String var) {
			return "(PROPERTY COLOR "+getColor()+" "+var+")";
		}
	}
	
	
	private Set<DCObject> getAllObjects() {
		Set<DCObject> ret=null;
		ret=addObjectsTo((Map)color2Objects,ret);
		ret=addObjectsTo((Map)shape2Objects,ret);
		ret=addObjectsTo((Map)size2Objects,ret);
		return ret;
	}
	
	private Set<DCObject> addObjectsTo(Map<Object, Set<DCObject>> indexStore, Set<DCObject> ret) {
		if (indexStore!=null) {
			for(Set<DCObject> vs:indexStore.values()) {
				if (vs!=null) {
					if (ret==null) ret=new HashSet<ObjectKB.DCObject>();
					ret.addAll(vs);
				}
			}
		}
		return ret;
	}

	public StringBuffer generateAxioms() {
		StringBuffer ret=null;
		Set<DCObject> objs = getAllObjects();
		if (objs!=null) {
			String var="x0";
			for(DCObject o:objs) {
				if (ret==null) ret=new StringBuffer();
				generateAxiomsForObject(o,var,ret);
			}
			//generateDisjointResolverAxiom(objs,ret);
		}
		return ret;
	}
	
	private void generateDisjointResolverAxiom(Set<DCObject> objs,StringBuffer ret) {
		if (objs!=null && !objs.isEmpty()) {
			String x="(B (_|_";
			for(DCObject o:objs) {
				x+=" (RESOLVED x0 "+o.getName().toUpperCase()+")";
			}
			x+="))";
			ret.append(x+"\n");
		}
	}

	private void generateAxiomsForObject(DCObject o, String var,StringBuffer ret) {
		ret.append("(B (name resolver-"+o.getName()+"-shape) (=> (RESOLVED "+var+" "+o.getName().toUpperCase()+" :1.1) "+o.getShapeAsLiteral(var)+"))");
		ret.append("\n");
		ret.append("(B (name resolver-"+o.getName()+"-size) (=> (RESOLVED "+var+" "+o.getName().toUpperCase()+" :1.1) "+o.getSizeAsLiteral(var)+"))");
		ret.append("\n");
		ret.append("(B (name resolver-"+o.getName()+"-color) (=> (RESOLVED "+var+" "+o.getName().toUpperCase()+" :1.1) "+o.getColorAsLiteral(var)+"))");
		ret.append("\n");
	}

	public static void main(String[] args) throws Exception {
		ObjectKB kb = new ObjectKB();
		DCObject o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		o = kb.completeProperties(SHAPE.BOX, SIZE.SMALL, null);
		kb.storeObject(o);
		System.out.println(kb.getAllObjects());
	}

}
