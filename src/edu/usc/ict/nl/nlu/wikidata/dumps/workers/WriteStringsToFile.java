package edu.usc.ict.nl.nlu.wikidata.dumps.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.util.FunctionalLibrary;

public class WriteStringsToFile extends Thread {

	private BlockingQueue<WikiThing> queue=null;
	private BlockingQueue<WikiThing> ret=null;
	private BufferedWriter dump;
	private int timeout=10;

	public WriteStringsToFile(String name,BlockingQueue<WikiThing> queue,BlockingQueue<WikiThing> ret,File dump,int timeout,boolean append) throws IOException {
		super(name);
		this.queue=queue;
		this.ret=ret;
		this.dump=new BufferedWriter(new FileWriter(dump,append));
		this.timeout=timeout;
	}

	@Override
	public void run() {
		while(true) {
			try {
				WikiThing p = queue.poll(timeout, TimeUnit.SECONDS);
				if (p!=null) {
					if (ret!=null) ret.put(p);
					List<String> things = p.getLabels();
					if (things!=null && !things.isEmpty()) {
						try {
							dump.write(p.getName());
							dump.write(FunctionalLibrary.printCollection(things, "\t", "\n", "\t"));
							dump.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else {
					try {
						dump.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.err.println("worker "+getName()+" exit.");
					break;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}
