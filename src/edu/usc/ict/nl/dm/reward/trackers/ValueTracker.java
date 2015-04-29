package edu.usc.ict.nl.dm.reward.trackers;

import java.util.concurrent.Semaphore;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.dm.reward.RewardDM;


public abstract class ValueTracker {
	protected Object value;
	private long timeLastChange;
	protected RewardDM dm;
	private final Semaphore lock=new Semaphore(1);
	
	public ValueTracker(RewardDM dm) {
		this.dm=dm;
		setter();
	}

	public Object getter() {return null;}
	/** 
	 * call this only inside the getter (doesn't use the lock)
	 * @param currentValue
	 */
	protected void setter(Object currentValue) {}
	public final void setter() {
		try {
			lock.acquire();
			Object currentValue=getter();
			setter(currentValue);
		} catch (InterruptedException e) {dm.getLogger().error(e);}
		finally {lock.release();}
	}
	
	public final void touch() {
		boolean locked=lock.tryAcquire();
		timeLastChange=System.currentTimeMillis();
		if (locked) lock.release();
	}
	/**
	 * use the lock to lock out the setter when using the getter and the getDelta
	 * @throws InterruptedException
	 */
	public void lock() throws InterruptedException {lock.acquire();}
	public void unlock() {lock.release();}
	
	public final float getDelta() {
		return (System.currentTimeMillis()-timeLastChange)/1000f;
	}
	
	public void updateIS() {}
}
