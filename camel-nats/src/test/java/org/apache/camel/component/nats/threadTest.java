package org.apache.camel.component.nats;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.spi.ExecutorServiceManager;


public class threadTest {
	

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		ExecutorService executor = Executors.newFixedThreadPool(2);
		NatsTestConnector[] connectors = new NatsTestConnector[2];
		
			
		for (int i = 0; i < 2; i++) {			
			NatsTestConnector connector = new NatsTestConnector();
			connectors[i] = connector;
			executor.submit(connector);
        }
		
		System.out.println("Started threads... " );
		Thread.sleep(1000);
		System.out.println("Main thread waking up... " );
		for (int i = 0; i < 2; i++) {			
			NatsTestConnector connector = connectors[i] ;
			connector.shutdown();
        }
		executor.shutdown();
	}

}
