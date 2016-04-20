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
		
		LOG.info("Instance Id: " + instanceId + "; Thread Id: " + threadName);
		
	}
	public void start () {
	
		Random randomGenerator = new Random();
		instanceId = randomGenerator.nextInt();	
		
		LOG.info("Starting Camel Route with Instance Id: " + instanceId );
		
	
	}

}
