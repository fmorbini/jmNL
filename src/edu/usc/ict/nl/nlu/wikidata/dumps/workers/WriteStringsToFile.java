package edu.usc.ict.nl.nlu.wikidata.dumps.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.util.FunctionalLibrary;

public class WriteStringsToFile extends Thread {

	private LinkedBlockingQueue<WikiThing> queue=null;
	private BufferedWriter dump;

	public WriteStringsToFile(String name,LinkedBlockingQueue<WikiThing> queue,File dump) throws IOException {
		super(name);
		this.queue=queue;
		this.dump=new BufferedWriter(new FileWriter(dump));
	}

	@Override
	public void run() {
		while(true) {
			try {
				WikiThing p = queue.poll(10, TimeUnit.SECONDS);
				if (p!=null) {
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
