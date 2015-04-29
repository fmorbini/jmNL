package edu.usc.ict.nl.dm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.util.StringUtils;

public class TimerThread extends Thread {
	
	public class TimerTask {
		long intervalInMilliseconds;
		long lastExeTime=System.currentTimeMillis();
		String name;
		Runnable task;
		private STATUS status=STATUS.RUNNING;
		private boolean oneTime=false;
		public TimerTask(String label,Runnable task,long interval, boolean oneTime) throws Exception {
			if (interval<=0) throw new Exception("invalid interval for timer task '"+label+"':"+interval);
			if (task==null) throw new Exception("null task for timer task '"+label+"'.");
			this.task=task;
			this.intervalInMilliseconds=interval;
			this.name=label;
			this.oneTime=oneTime;
		}
		String getName() {return name;}
		public long getLastExecutionTime() {return lastExeTime;}
		public void setLastExecutionTime(long time) {this.lastExeTime=time;}
		public long getIntervalInMilliseconds() {return intervalInMilliseconds;}
		public Runnable getTask() {return task;}
		public boolean canRun() {return !isPaused() && !isKilled();}
		private boolean isKilled() {return this.status==TimerThread.STATUS.KILLED;}
		private boolean isPaused() {return this.status==TimerThread.STATUS.PAUSED;}
		public void run() {
			getTask().run();
			if (oneTime) this.status=TimerThread.STATUS.KILLED;
		}
		
		@Override
		public String toString() {
			return "["+status+"] '"+getName()+"' every "+getIntervalInMilliseconds()+" [ms] "+((oneTime)?"single":"periodic");
		}
	}
	
	private enum STATUS {PAUSED,RUNNING,KILLED}

	private DM dm=null;
	private boolean run=true;
	private STATUS status=STATUS.RUNNING;
	
	LinkedHashMap<String,TimerTask> tasks=new LinkedHashMap<String, TimerThread.TimerTask>();
	
	public TimerThread(String name,DM r) throws Exception {
		super(name);
		if (r==null) throw new Exception("Timer thread started with null dialogue manager.");
		this.dm=r;
		this.dm.getLogger().info("Starting timer: "+name);
		start();
	}

	public boolean hasTaskNamed(String name) {
		if (tasks!=null) return tasks.containsKey(name);
		else return false;
	}
	/** return value: negative: overdue, null: no task or cannot execute */
	public Long getRemainingMillisecondsToExeTaskNamed(String name) {
		if (tasks!=null) {
			TimerTask tt=tasks.get(name);
			if (tt!=null) {
				if (tt.canRun()) {
					long currentTime=System.currentTimeMillis();
					long lastExecutionTime=tt.getLastExecutionTime();
					long taskInterval=tt.getIntervalInMilliseconds();
					long currentDelta=currentTime-lastExecutionTime;
					return taskInterval-currentDelta;
				} else return null;
			} else return null;
		} else return null;
	}
	
	public void addTask(TimerTask t) throws Exception {
		if (t!=null) {
			if (tasks==null) tasks=new LinkedHashMap<String, TimerThread.TimerTask>();
			String name=t.getName();

			if (tasks.containsKey(name)) dm.getLogger().warn("overwriting task: "+t);
			else dm.getLogger().info("Task added to timer: "+t);
			tasks.put(name, t);
		}
	}
	public void removeTask(String name) {
		if (tasks!=null) {
			if (tasks.containsKey(name)) {
				dm.getLogger().info("Task '"+name+"' removed from timer.");
				tasks.remove(name);
			}
		}
	}

	private final List<String> toBeRemoved=new ArrayList<String>();
	public void run() {
		try {
			while(!isKilled()){
				if (tasks!=null && canRun()) {
					toBeRemoved.clear();
					for(TimerTask t:tasks.values()) {
						if (t.canRun()) {
							long currentTime=System.currentTimeMillis();
							long lastExecutionTime=t.getLastExecutionTime();
							long taskInterval=t.getIntervalInMilliseconds();
							long currentDelta=currentTime-lastExecutionTime;
							if (currentDelta>=taskInterval) {
								// adjust new starting time reference with the time lost. 
								t.setLastExecutionTime(currentTime-(currentDelta-taskInterval));
								t.run();
							}
						} else if (t.isKilled()) toBeRemoved.add(t.getName()); 
					}
					for(String n:toBeRemoved) removeTask(n);
				}
				Thread.sleep(100);
			}
		} catch (Exception t) {
			t.printStackTrace();
		}
		dm.getLogger().warn("Terminated timer thread: "+getName());
	}


	public boolean isTimerEvent(Event ev) {
		if (ev!=null) {
			String evName=ev.getName();
			if (!StringUtils.isEmptyString(evName)) return evName.equals(dm.getConfiguration().getTimerEvent());
		}
		return false;
	}

	private boolean isKilled() {return this.status==STATUS.KILLED;}
	private boolean isPaused() {return this.status==STATUS.PAUSED;}
	public void setAsKilled() {this.status=STATUS.KILLED;}
	public void setAsPaused() {this.status=STATUS.PAUSED;}
	public void setAsRunning() {this.status=STATUS.RUNNING;}
	public boolean canRun() {return !isKilled() && !isPaused();}

	private int next=0;
	private final String prefix="task";
	public String getNewName() {
		next++;
		if (tasks!=null) {
			String name=prefix+next;
			while(tasks.containsKey(name)) {
				next++;
				name=prefix+next;
			}
			return name;
		} else return prefix+next;
	}
	
	public void scheduleTaskIn(String label, Runnable runnable, long ms, boolean oneTime) throws Exception {
		TimerTask tt = new TimerTask(label, runnable, ms,oneTime);
		addTask(tt);
	}
}

