package com.logi.camel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;

public class NatsConnection {
	private static final Logger LOG = LoggerFactory.getLogger(NatsConnection.class);
	
	public Connection connection = null;
	public NatsConnection (){
		
		String url = System.getenv("NATS_URI");
		//String url1 = System.getenv("NATSSERVERINT_URI");
		String defaultUrl ="nats://0.0.0.0:4222";
		LOG.info("Apcera URl: " + url);
		
		if (url == null)
			url = defaultUrl;
		
		LOG.info("Trying to connect to url: " + url);
		
		ConnectionFactory cf = new ConnectionFactory(url);
		 try  {
			 connection = cf.createConnection();
	        }
	        catch(Exception e)
	        {
	        	e.printStackTrace();
	        }
		 LOG.info("Connected to: " + url);
	}

}
