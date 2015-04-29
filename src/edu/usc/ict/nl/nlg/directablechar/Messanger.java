package edu.usc.ict.nl.nlg.directablechar;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import edu.usc.ict.nl.nlu.directablechar.ReplyMessageProcessor;
import edu.usc.ict.nl.vhmsg.VHBridge;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class Messanger {
	private VHBridge vhBridge;

	public Messanger() {
		vhBridge=new VHBridge("localhost", "DEFAULT_SCOPE");
	}
	
	public void waitForReply(String replyMessageID,String messageType,String messageBody,final ReplyMessageProcessor p,int secondsTimeout) {
		final Semaphore lock=new Semaphore(0);
		final MessageListener l = new MessageListener() {
			@Override
			public void messageAction(MessageEvent event) {
				p.processMessage(event);
				lock.release();
			}
		};
		if (p!=null) vhBridge.addMessageListenerFor(replyMessageID, l);
		vhBridge.sendMessage(messageType, messageBody);
		if (p!=null) {
			try {
				boolean r = lock.tryAcquire(secondsTimeout, TimeUnit.SECONDS);
				if (!r) System.err.println("timeout");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			vhBridge.removeMessageListener(l);
		}
	}

	public void kill() {
		if (vhBridge!=null) vhBridge.kill();
	}
}
