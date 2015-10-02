package edu.usc.ict.nl.nlg.directablechar;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.nlg.echo.EchoNLG;
import edu.usc.ict.nl.nlu.directablechar.LFNLU2;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.COLOR;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.DCObject;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.SHAPE;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.SIZE;
import edu.usc.ict.nl.nlu.directablechar.ReplyMessageProcessor;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.vhmsg.MessageEvent;

public class DirectableChar extends EchoNLG {

	private Messanger msg=null;
	private static final String base="object";
	private static int objectCounter=1;

	public DirectableChar(NLGConfig c) {
		super(c);
		this.msg=new Messanger();
	}

	@Override
	public NLGEvent doNLG(Long sessionID, DMSpeakEvent ev, SpeechActWithProperties line, boolean simulate) throws Exception {
		NLBusInterface m = getNLModule();
		NLUInterface nlu = m.getNlu(sessionID);
		boolean pick=false;
		if (nlu!=null && nlu instanceof LFNLU2) {
			String name=ev.getName();
			ObjectKB objectKB = ((LFNLU2)nlu).getObjectKB();
			if (name.equals("RCREATE")) {
				DialogueKBInterface is = ev.getLocalInformationState();
				if (is!=null) {
					SHAPE shape = getValueOfEnum(is,"SHAPE", SHAPE.class);
					SIZE size = getValueOfEnum(is,"SIZE", SIZE.class);
					COLOR color = getValueOfEnum(is,"COLOR", COLOR.class);
					DCObject ob = createObject(objectKB,null, color, shape, size, null);
					return new NLGEvent(name+"("+ob+")", sessionID, ev);
				}
			} else if ((pick=name.equals("PICK")) || name.equals("GOTO")) {
				DialogueKBInterface is = ev.getLocalInformationState();
				if (is!=null) {
					String objectName=null;
					Object ov=is.get("ARG0");
					if (ov!=null) {
						ov=is.evaluate(ov, null);
						if (ov!=null && ov instanceof String) {
							objectName=DialogueKBFormula.getStringValue((String)ov);
						}
					}
					if (!StringUtils.isEmptyString(objectName)) {
						boolean found=gotoObject(objectName,pick);
						if (pick) {
							DIR pos=didIGrabThisObject(objectName);
							if (pos!=null) {
								return new NLGEvent(name+"("+objectName+" with "+pos+" hand)", sessionID, ev);
							} else {
								return new NLGEvent("failed to grab '"+objectName+"'", sessionID, ev);
							}
						} else {
							if (found) {
								return new NLGEvent(name+"("+objectName+")", sessionID, ev);
							} else {
								return new NLGEvent("'"+objectName+"' not found", sessionID, ev);
							}
						}
					}
				}
			} else if (name.equals("DELETE")) {
				DialogueKBInterface is = ev.getLocalInformationState();
				if (is!=null) {
					String objectName=null;
					Object ov=is.get("ARG0");
					if (ov!=null) {
						ov=is.evaluate(ov, null);
						if (ov!=null && ov instanceof String) {
							objectName=DialogueKBFormula.getStringValue((String)ov);
						}
					}
					if (!StringUtils.isEmptyString(objectName)) {
						boolean found=deleteObject(objectName);
						if (found) {
							objectKB.removeObjectNamed(objectName);
							return new NLGEvent(name+"("+objectName+")", sessionID, ev);
						} else {
							return new NLGEvent("'"+objectName+"' not found", sessionID, ev);
						}
					}
				}
			} else if (name.equals("RELEASE")) {
				DialogueKBInterface is = ev.getLocalInformationState();
				if (is!=null) {
					String objectName=null;
					Object ov=is.get("ARG0");
					if (ov!=null) {
						ov=is.evaluate(ov, null);
						if (ov!=null && ov instanceof String) {
							objectName=DialogueKBFormula.getStringValue((String)ov);
						}
					}
					if (!StringUtils.isEmptyString(objectName)) {
						Float[] dropPos = getPositionOfDrop(objectName, objectKB);
						boolean released=releaseObject(objectName,dropPos);
						if (released) {
							return new NLGEvent(name+"("+objectName+")", sessionID, ev);
						} else {
							return new NLGEvent("failed to release '"+objectName+"'", sessionID, ev);
						}
					}
				}
			} else {
				return super.doNLG(sessionID, ev, line,simulate);
			}
		}
		return null;
	}
	public <E extends Enum<E>> E getValueOfEnum(DialogueKBInterface is,String name,Class<E> enumType) {
		try {
			Object ov=is.get(name);
			if (ov!=null) {
				ov=is.evaluate(ov, null);
				if (ov!=null && ov instanceof String) {
					return Enum.valueOf(enumType,DialogueKBFormula.getStringValue((String)ov));
				}
			}
		} catch (Exception ex) {}
		return null;
	}
	
	public DCObject createObject(ObjectKB objectKB, String name,ObjectKB.COLOR color,ObjectKB.SHAPE shape,ObjectKB.SIZE sizeMod,float[] xyz) throws Exception {
		Set<String> obs=getListOfObjectsInWorld();
		
		// get the name
		if (StringUtils.isEmptyString(name)) name=base;
		while(obs.contains(name)) {
			name=base+objectCounter;
			objectCounter++;
		}
			
		//complete other properties
		DCObject obj = objectKB.completeProperties(shape, sizeMod, color);
		obj.setProperty(name);
		
		shape=obj.getShape();
		sizeMod=obj.getSize();
		color=obj.getColor();
		Color awtColor;
		try {
		    Field field = Color.class.getField(color.toString());
		    awtColor = (Color)field.get(null);
		} catch (Exception e) {
			awtColor = null;
		}
		
		float[] s=ObjectKB.getSizeForShape(shape,sizeMod);
		Random rg=new Random();
		//position
		if (xyz==null) {
			xyz=new float[3];
			xyz[0]=(rg.nextFloat()-0.5f)*8f;
			xyz[2]=(rg.nextFloat()-0.5f)*8f;
			xyz[1]=0+s[1];
		}
		
		//get the color
		float r=(float)awtColor.getRed()/(float)255;
		float g=(float)awtColor.getGreen()/(float)255;
		float b=(float)awtColor.getBlue()/(float)255;
		final String id = UUID.randomUUID().toString();
		msg.waitForReply(id, "sb", "myobject = scene.createPawn(\""+name+"\");myobject.setStringAttribute(\"collisionShape\", \""+shape.toString().toLowerCase()+"\");myobject.setVec3Attribute(\"collisionShapeScale\", "+s[0]+", "+s[1]+", "+s[2]+");myobject.setVec3Attribute(\"color\", "+r+", "+g+", "+b+");myobject.setPosition(SrVec("+xyz[0]+", "+xyz[1]+", "+xyz[2]+"))", null, 10);
		obs=getListOfObjectsInWorld();
		if (obs.contains(name)) objectKB.storeObject(obj);  
		return obj;
	}

	public Set<String> getListOfObjectsInWorld() throws InterruptedException {
		final Set<String> ret=new HashSet<String>();
		final String id = UUID.randomUUID().toString();
		ReplyMessageProcessor replyListener = new ReplyMessageProcessor() {
			@Override
			public void processMessage(MessageEvent e) {
				Map<String, ?> map = e.getMap();
				if(map.containsKey(id)){
					String msg=(String) map.get(id);
					if (!StringUtils.isEmptyString(msg)) {
						String[] objects=msg.split("\t");
						ret.addAll(Arrays.asList(objects));
					}
				}
			}
		};
		msg.waitForReply(id, "sb","objects = scene.getPawnNames();str = '\t'.join(objects);scene.vhmsg2(\""+id+"\", str)",replyListener,3);
		return ret;
	}
	public Float[] getPositionForObject(String name) {
		final Float[] ret=new Float[]{null,null,null};
		final String id = UUID.randomUUID().toString();
		ReplyMessageProcessor replyListener = new ReplyMessageProcessor() {
			@Override
			public void processMessage(MessageEvent e) {
				Map<String, ?> map = e.getMap();
				if(map.containsKey(id)){
					String msg=(String) map.get(id);
					if (!StringUtils.isEmptyString(msg)) {
						String[] objects=msg.split("[\\s]+");
						if (objects!=null && objects.length==3) {
							for(int i=0;i<ret.length;i++) ret[i]=Float.parseFloat(objects[i]);
						}
					}
				}
			}
		};
		msg.waitForReply(id, "sb","c = scene.getPawn(\""+name.toLowerCase()+"\");p=c.getPosition() if c else None;str = ('%.2f'%p.getData(0)+\" \"+'%.2f'%p.getData(1)+\" \"+'%.2f'%p.getData(2)) if p else 'null';scene.vhmsg2(\""+id+"\", str)",replyListener,10);
		return ret;
	}
	public Float[] getPositionOfDrop(String objectName, ObjectKB objectKB) {
		if (!StringUtils.isEmptyString(objectName)) {
			objectName=objectName.toLowerCase();
			Float[] current=getPositionForObject(objectName);
			if (current[0]!=null && current[2]!=null && objectKB!=null) {
				DCObject obj=objectKB.getObjectNamed(objectName);
				if (obj!=null) {
					SHAPE shape = obj.getShape();
					SIZE sizeMod = obj.getSize();
					float[] sizeDims = ObjectKB.getSizeForShape(shape, sizeMod);
					current[1]=sizeDims[1];
					return current;
				}
			}
		}
		return null;
	}
	
	public enum DIR {RIGHT,LEFT};
	public DIR didIGrabThisObject(String name) {
		DIR ret=DIR.RIGHT;
		String gname=getNameOfGrabbedObject(ret);
		if (gname==null || !gname.equalsIgnoreCase(name)) gname=getNameOfGrabbedObject(ret=DIR.LEFT);
		if (gname!=null) return ret;
		else return null;
	}
	public String getNameOfGrabbedObject() {
		String name=getNameOfGrabbedObject(DIR.RIGHT);
		if (name!=null) return name;
		return getNameOfGrabbedObject(DIR.LEFT);
	}
	public String getNameOfGrabbedObject(DIR dir) {
		//return null;
		
		final String[] ret=new String[1];
		final String id = UUID.randomUUID().toString();
		ReplyMessageProcessor replyListener = new ReplyMessageProcessor() {
			@Override
			public void processMessage(MessageEvent e) {
				Map<String, ?> map = e.getMap();
				if(map.containsKey(id)){
					String msg=(String) map.get(id);
					if (!StringUtils.isEmptyString(msg)) {
						ret[0]=(msg.equalsIgnoreCase("null"))?null:msg;
					}
				}
			}
		};
		msg.waitForReply(id, "sb","c = scene.getCharacter(\"ChrBrad\");p=c.getReachAttachedPawnName(\""+dir.toString().toLowerCase()+"\") if c else None;scene.vhmsg2(\""+id+"\", p)",replyListener,3);
		return ret[0];
		
	}

	public boolean gotoObject(String name,boolean pick) throws InterruptedException {
		name=name.toLowerCase();
		Set<String> obs = getListOfObjectsInWorld();
		if (obs.contains(name)) {
			final String id = UUID.randomUUID().toString();
			if (pick) {
				msg.waitForReply(id, "sb", "bml.execBML('ChrBrad', '<sbm:reach sbm:action=\"pick-up\" sbm:reach-duration=\"0.2\" sbm:use-locomotion=\"true\" target=\""+name+"\"/>')", null, 10);
			} else {
				msg.waitForReply(id, "sb", "bml.execBML('ChrBrad', '<locomotion target=\""+name.toLowerCase()+"\"/>');scene.vhmsg2(\""+id+"\", \"done\")", null, 10);
			}
			return true;
		}
		return false;
	}
	public boolean deleteObject(String name) throws InterruptedException {
		name=name.toLowerCase();
		Set<String> obs = getListOfObjectsInWorld();
		if (obs.contains(name)) {
			final String id = UUID.randomUUID().toString();
			msg.waitForReply(id, "sb", "scene.removePawn(\""+name.toLowerCase()+"\")", null, 10);
			return true;
		}
		return false;
	}
	
	public boolean releaseObject(String name, Float[] dropPos) {
		if (!StringUtils.isEmptyString(name)) {
			DIR dir=didIGrabThisObject(name);
			if (dir!=null) {
				final String id = UUID.randomUUID().toString();
				msg.waitForReply(id, "sb", "bml.execBML('ChrBrad', '<sbm:reach sbm:action=\"put-down\" sbm:reach-type=\""+dir.toString().toLowerCase()+"\" sbm:reach-duration=\"0.2\" sbm:use-locomotion=\"true\" sbm:target-pos=\""+dropPos[0]+" "+dropPos[1]+" "+dropPos[2]+"\"/>')", null, 10);
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) throws InterruptedException {
		DirectableChar dc = new DirectableChar(NLGConfig.WIN_EXE_CONFIG);
		System.out.println(dc.getListOfObjectsInWorld());
		System.out.println(Arrays.toString(dc.getPositionForObject("object1")));
	}
	
	/*
	 * ~ c = scene.getCharacter("ChrBrad")

~ pos = c.getPosition()
~ print pos.getData(0)
-0.174194857478

~ print pos.getData(2)
0.171502828598

reach after locomotion


   pawn released: object


	 */


}
