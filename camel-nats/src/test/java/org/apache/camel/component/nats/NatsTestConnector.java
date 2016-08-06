package org.apache.camel.component.nats;

public class NatsTestConnector implements Runnable{

	private volatile boolean running = true;
	private Object runningLock = null;
	
	public NatsTestConnector() {
		this.runningLock  = new Object();
	}
	@Override
	public void run() {
		while (running){
			
			synchronized(runningLock)
            {
                 try {
                     runningLock.wait();
                 }
                 catch (InterruptedException e) {
                	 System.out.println("Interrupted Exception" );
                 }
             }
			
			System.out.println("Stopped waiting for lock with run = " + Boolean.toString(running));
		}
		System.out.println("Broked out of loop");
	}
	public void shutdown(){
		running = false;
		synchronized(runningLock)
        {
			System.out.println("Notifying lock on shutdown " );
			runningLock.notifyAll();
        }
	}
}

