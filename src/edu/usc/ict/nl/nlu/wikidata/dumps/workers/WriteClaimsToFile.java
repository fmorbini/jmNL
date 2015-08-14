package edu.usc.ict.nl.nlu.wikidata.dumps.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.usc.ict.nl.nlu.wikidata.WikiClaim;
import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.util.FunctionalLibrary;

public class WriteClaimsToFile extends Thread {

	private LinkedBlockingQueue<WikiThing> queue=null;
	private BlockingQueue<WikiThing> ret=null;
	private BufferedWriter dump;

	public WriteClaimsToFile(String name,LinkedBlockingQueue<WikiThing> queue,LinkedBlockingQueue<WikiThing> ret,File dump) throws IOException {
		super(name);
		this.queue=queue;
		this.ret=ret;
		this.dump=new BufferedWriter(new FileWriter(dump));
	}

	@Override
	public void run() {
		while(true) {
			try {
				WikiThing p = queue.poll(10, TimeUnit.SECONDS);
				if (p!=null) {
					if (ret!=null) ret.put(p);
					List<WikiClaim> claims = p.getClaims();
					if (claims!=null && !claims.isEmpty()) {
						for(WikiClaim cl:claims) {
							if (cl!=null) {
								try {
									dump.write(cl.getProperty()+"\t"+cl.getSubject()+"\t"+cl.getObject()+"\n");
									dump.flush();
								} catch (Exception e) {e.printStackTrace();}
							}
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
