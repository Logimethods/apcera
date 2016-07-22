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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.nats.client.ConnectionFactory;
import io.nats.client.Message;
import io.nats.client.Subscription;
import io.nats.connector.Connector;
import io.nats.connector.plugin.NATSConnector;
import io.nats.connector.plugin.NATSConnectorPlugin;
import io.nats.connector.plugin.NATSEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsConsumer extends DefaultConsumer implements NATSConnectorPlugin{

    private static Logger logger = LoggerFactory.getLogger(NatsConsumer.class);

    private final Processor processor;
    private ExecutorService executor;
    NATSConnector natsConnector = null;
    
    private Subscription sid;
    private boolean subscribed;
    private CountDownLatch startupLatch = null;
    private CountDownLatch shutdownLatch = null;

	private Connector connector;

    public NatsConsumer(NatsEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.processor = processor;
    }

    @Override
    public NatsEndpoint getEndpoint() {
        return (NatsEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        logger.debug("Starting Nats Consumer");
        executor = getEndpoint().createExecutor();

        logger.debug("Getting Nats Connection");
        //connection = getConnection();
        NatsConfiguration config = getEndpoint().getNatsConfiguration();       	 
   	 	connector = new Connector(config.createProperties());
   	 	connector.setPlugin(this);

        //executor.submit(new NatsConsumingTask(connection, getEndpoint().getNatsConfiguration()));
   	 	startupLatch = new CountDownLatch(1);
   	 	executor.submit(connector);
   	 	
   	 	// Wait for connector to fully initialize
        boolean initialized = false;
        try{
        	initialized = startupLatch.await(5, TimeUnit.SECONDS);
        }
        catch(InterruptedException e){
        	logger.error("Nats consumer initilization was interrupted"); 
        	throw new Exception("Startup failure in NATS Connector");
        }
        
        if (initialized == false){
        	logger.info("Nats Consumer initilization is taking longer then expected"); 
        	//throw new Exception("Startup failure in NATS Connector");
        }
        
        logger.info("Started NATS Consumer");
    }

    @Override
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
         	logger.error("Nats consumer shutdown was interrupted"); 
         }
         
         if (shutdown == false){
         	logger.error("Nats Consumer shutdown timed out"); 
         }
                  
         super.doStop();
         logger.debug("Stopping Nats Consumer");
         if (executor != null) {
            if (getEndpoint() != null && getEndpoint().getCamelContext() != null) {
                getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(executor);
            } else {
                executor.shutdownNow();
            }
         }
         executor = null;      
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }
  
    @Override
	public boolean onStartup(Logger logger, ConnectionFactory factory) {
		  //this.logger = logger;
		  logger.info("Received NATS consumer onStartup event");
	      return true;
	}

	@Override
	public boolean onNatsInitialized(NATSConnector connector) {
		logger.info("Received NATS consumer onInitialized event");
		
		this.natsConnector = connector;		
		
	        try
	        {
	        	if (ObjectHelper.isNotEmpty(getEndpoint().getNatsConfiguration().getQueueName())) {
	        		natsConnector.subscribe(getEndpoint().getNatsConfiguration().getTopic(),
	        				getEndpoint().getNatsConfiguration().getQueueName());
	        		if (ObjectHelper.isNotEmpty(getEndpoint().getNatsConfiguration().getMaxMessages())) {
	        			natsConnector.autoUnsubscribe(getEndpoint().getNatsConfiguration().getTopic(),
	        					Integer.parseInt(getEndpoint().getNatsConfiguration().getMaxMessages()));
	        		}
	        		setSubscribed(true);
	        	}
	        	else{
	        		connector.subscribe(getEndpoint().getNatsConfiguration().getTopic());
	        		if (ObjectHelper.isNotEmpty(getEndpoint().getNatsConfiguration().getMaxMessages())) {
	        			natsConnector.autoUnsubscribe(getEndpoint().getNatsConfiguration().getTopic(),
	        					Integer.parseInt(getEndpoint().getNatsConfiguration().getMaxMessages()));
	        		}
	        		setSubscribed(true);
	        	}
	        }
	        catch (Throwable e) {
	        	logger.error("Unable to subscribe");
	        	getExceptionHandler().handleException("Error during processing", e);
	        }
	        
	        if (startupLatch != null)
				startupLatch.countDown();
	        
	        return true;
	}

	@Override
	public void onNATSMessage(Message msg) {
		logger.info("Received message: " + msg.toString());
		 Exchange exchange = getEndpoint().createExchange();
         exchange.getIn().setBody(msg);
         exchange.getIn().setHeader(NatsConstants.NATS_MESSAGE_TIMESTAMP, System.currentTimeMillis());
         exchange.getIn().setHeader(NatsConstants.NATS_SUBSCRIPTION_ID, sid);
         try {
             processor.process(exchange);
         } catch (Exception e) {
             getExceptionHandler().handleException("Error during processing", exchange, e);
         }	
	}

	@Override
	public void onNATSEvent(NATSEvent event, String message) {
		logger.info("Received NATS consumer onNATSEvent event");
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
	}

	@Override
	public void onShutdown() {		
	     logger.info("Consumer is Shutting down.");
	    	        
	     try {
	    	 natsConnector.flush();
	    	 natsConnector.unsubscribe(getEndpoint().getNatsConfiguration().getTopic());
	     } catch (Exception e) {
	         getExceptionHandler().handleException("Error during unsubscribing", e);
	     }		
	}
}
	
	