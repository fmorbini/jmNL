package edu.usc.ict.nl.nlu.wikidata.dumps.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import edu.usc.ict.nl.nlu.wikidata.WikiClaim;
import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.util.FunctionalLibrary;

public class WriteClaimsToFile extends Thread {

	private LinkedBlockingQueue<WikiThing> queue=null;
	private BufferedWriter dump;

	public WriteClaimsToFile(LinkedBlockingQueue<WikiThing> queue,File dump) throws IOException {
		this.queue=queue;
		this.dump=new BufferedWriter(new FileWriter(dump));
	}

	@Override
	public void run() {
		while(true) {
			try {
				WikiThing p = queue.take();
				if (p!=null) {
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
					break;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}
