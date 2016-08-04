package com.logi.camel;



import java.util.Random;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Tracer{
	
	private static final Logger LOG = LoggerFactory.getLogger(Tracer.class);
	private static int instanceId = 0;

	public void trace(Exchange exchange) throws Exception {
	
		String threadName = (String)exchange.getIn().getHeader("THREAD_NAME");		
		//LOG.info("Instance Id: " + instanceId + "; Thread Id: " + threadName);
		
		String message = (String)exchange.getIn().getBody(String.class);
		LOG.info("Processing Message: " + message.toString()+ " On thread: " + threadName);
		Thread.sleep(2000);
		
	}
	public void start () {
	
		Random randomGenerator = new Random();
		instanceId = randomGenerator.nextInt();	
		
		LOG.info("Starting Camel Route with Instance Id: " + instanceId );
		
	
	}

}
