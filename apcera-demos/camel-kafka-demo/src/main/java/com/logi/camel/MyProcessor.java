package com.logi.camel;

import org.apache.camel.component.kafka.KafkaConstants;


import java.util.Random;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyProcessor{
	
	private static final Logger LOG = LoggerFactory.getLogger(MyProcessor.class);
	private static int instanceId = 0;
	private static String kafkaPartition = "0";


	public void process(Exchange exchange) throws Exception {
	
		exchange.getIn().setHeader(KafkaConstants.KEY, "SENSOR_DATA");
		String threadName = (String)exchange.getIn().getHeader("THREAD_NAME");
		
		LOG.info("Instance Id: " + instanceId + "; Thread Id: " + threadName);
		
	}
	public void start () {
	
		Random randomGenerator = new Random();
		instanceId = randomGenerator.nextInt();	
		
		LOG.info("Starting Camel Route with Instance Id: " + instanceId );
		
	
	}

}
