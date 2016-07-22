/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.nats;

import io.nats.client.ConnectionFactory;
import io.nats.client.Message;
import io.nats.connector.Connector;
import io.nats.connector.plugin.NATSConnector;
import io.nats.connector.plugin.NATSConnectorPlugin;
import io.nats.connector.plugin.NATSEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsProducer extends DefaultProducer implements NATSConnectorPlugin{
    
    private static final Logger logger = LoggerFactory.getLogger(NatsProducer.class);
    private CountDownLatch startupLatch = null;
    private CountDownLatch shutdownLatch = null;
    
    private NATSConnector natsConnector = null;
    Connector connector = null;
    
    public NatsProducer(NatsEndpoint endpoint) {
        super(endpoint);
 
        try {
        	 NatsConfiguration config = endpoint.getNatsConfiguration();       	 
        	 connector = new Connector(config.createProperties());
        	 connector.setPlugin(this);
		} catch (Exception e) {
			logger.error("Error creating Connctor object");
			e.printStackTrace();
		}        
    }
    
    @Override //DefaultProducer
    public NatsEndpoint getEndpoint() {
        return (NatsEndpoint) super.getEndpoint();
    }
    
    @Override //DefaultProducer
    public void process(Exchange exchange) throws Exception {
        NatsConfiguration config = getEndpoint().getNatsConfiguration();
        String body = exchange.getIn().getMandatoryBody(String.class);

        logger.info("Publishing to topic: {}", config.getTopic());
        
       
        String replySubject = config.getReplySubject();
           
        if (ObjectHelper.isNotEmpty(config.getReplySubject())) {
            publish(config.getTopic(), replySubject, body.getBytes());
        } else {
        	publish(config.getTopic(), null, body.getBytes());
        }
    }
    
    public void publish(String subject, String replySubject, byte[] payload) throws Exception{
		
		if (natsConnector == null){
			logger.error("NATS connection is not initialized");
		    throw new Exception("Invalid State Nats Connector is null");
		}      
		
		Message m = new Message();
		m.setSubject(subject);
	        
		if (replySubject != null)
	        m.setReplyTo(replySubject);
	        
		m.setData(payload, 0, payload.length);
	
		natsConnector.publish(m);
	
		try {
			natsConnector.flush();
	    }
	    catch (Exception e){
	       	logger.error("Error with flush:  ", e);
	    }
	}
    
    @Override //DefaultProducer
    protected void doStart() throws Exception {
        super.doStart();
        logger.debug("Starting Nats Producer");    
        startupLatch = new CountDownLatch(1);
        
        try {
           (new Thread(connector)).start();
        } catch (Exception e) {
        	logger.error("Can not start Nats Connector thread"); 
        	throw new Exception("Startup failure in NATS Connector");
        }
        // Wait for connector to fully initialize
        boolean initialized = false;
        try{
        	initialized = startupLatch.await(5, TimeUnit.SECONDS);
        }
        catch(InterruptedException e){
        	logger.error("Nats Producer initilization was interrupted"); 
        	throw new Exception("Startup failure in NATS Connector");
        }
        
        if (initialized == false){
        	logger.info("Nats Producer initilization is taking longer then expected"); 
        	//throw new Exception("Startup failure in NATS Connector");
        }
        
        logger.info("Started NATS Producer");
    }

    @Override  //DefaultProducer
    protected void doStop() throws Exception {
       
    
        
        shutdownLatch = new CountDownLatch(1);

        if (connector != null) {
        	connector.shutdown();
        }
        
        boolean shutdown = false;
        try{
        	shutdown = shutdownLatch.await(5, TimeUnit.SECONDS);
        }
        catch(InterruptedException e){
        	logger.error("Nats Producer shutdown was interrupted"); 
        }
        
        if (shutdown == false){
        	logger.error("Nats Producer shutdown timed out"); 
        }
        
        logger.info("Stopping Nats Producer");
        super.doStop();
    }

	@Override //NATSConnectorPlugin
	public boolean onStartup(Logger logger, ConnectionFactory factory) {
		logger.info("Received NATS connector onStartup event");
		return true;
	}

	@Override //NATSConnectorPlugin
	public boolean onNatsInitialized(NATSConnector connector) {
		this.natsConnector = connector;
		if (startupLatch != null)
			startupLatch.countDown();

		logger.info("Received NATS connector onInitialized event");
		return true;
	}

	@Override //NATSConnectorPlugin
	public void onNATSMessage(Message msg) {
		logger.info("Received NATS connector onNATSMessage event");
		
	}

	@Override //NATSConnectorPlugin
	public void onNATSEvent(NATSEvent event, String message) {
		switch (event)
        {
            case ASYNC_ERROR:
                logger.error("NATS Event Async error: " + message);
                break;
            case RECONNECTED:
                logger.info("NATS Event Reconnected: " + message);
                break;
            case DISCONNECTED:
                logger.info("NATS Event Disconnected: " + message);
                break;
            case CLOSED:
                logger.info("NATS Event Closed: " + message);
                if(shutdownLatch != null)
                	shutdownLatch.countDown();
                break;
            default:
                logger.info("NATS Event Unrecognized event: " + message);
        }

        // throw exceptions to ensure the framework handles them.
        //throw new RuntimeException("Test framework plugin exception handling.");
			
	}

	@Override //NATSConnectorPlugin
	public void onShutdown() {
		logger.info("Received NATS connector onShutdown event");	

	}

}
