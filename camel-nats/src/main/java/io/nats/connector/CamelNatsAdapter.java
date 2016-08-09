package io.nats.connector;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.apache.camel.Exchange;
import org.apache.camel.component.nats.NatsConstants;
import org.apache.camel.component.nats.NatsConsumer;
import org.apache.camel.component.nats.NatsProducer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;

import io.nats.client.ConnectionEvent;
import io.nats.client.Message;
import io.nats.client.NATSException;
import io.nats.client.Subscription;

public class CamelNatsAdapter {
	
	private NatsConnector natsConnector;
	private Logger logger;	
	private NatsConsumer natsConsumer = null;
	private NatsProducer natsProducer = null;
	private Subscription sid;
	
	enum AdapterType{
		PRODUCER,
		CONSUMER
	};
	AdapterType adapterType;
	
	public CamelNatsAdapter(NatsConsumer natsConsumer, Properties natsProperties, Logger logger) {
		this.natsConsumer = natsConsumer;
		this.adapterType = AdapterType.CONSUMER;
		this.natsConnector = new NatsConnector(this, natsProperties, logger);
		this.logger = logger;
	}

	public CamelNatsAdapter(NatsProducer natsProducer, Properties natsProperties, Logger logger) {
		this.natsProducer = natsProducer;
		this.logger = logger;
		this.adapterType = AdapterType.PRODUCER;
		this.natsConnector = new NatsConnector(this, natsProperties, logger);
	}
	
	public boolean onNatsInitialized(NatsConnector natsConnector, Logger logger) {	
		
		if(adapterType == AdapterType.PRODUCER){
			logger.info("Received NATS producer onInitialized event");
			if (natsProducer.getStartupLatch() != null)
				natsProducer.getStartupLatch().countDown();
		}
		else if(adapterType == AdapterType.CONSUMER){
			logger.info("Received NATS consumer onInitialized event");
	        try
	        {
	        	if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getQueueName())) {
	        		natsConnector.subscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
	        				natsConsumer.getEndpoint().getNatsConfiguration().getQueueName());
	        		if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages())) {
	        			natsConnector.autoUnsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
	        					Integer.parseInt(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages()));
	        		}
	        	}
	        	else{
	        		natsConnector.subscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
	        		if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages())) {
	        			natsConnector.autoUnsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
	        					Integer.parseInt(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages()));
	        		}
	        	}
	        }
	        catch (Throwable e) {
	        	logger.error("Unable to subscribe");
	        	natsConsumer.getExceptionHandler().handleException("Error during processing", e);
	        }
	        
	        if (natsConsumer.getStartupLatch() != null)
	        	natsConsumer.getStartupLatch().countDown();
		}			
	    return true;
	}
	
	public void onNATSMessage(Message msg) {
				
		logger.info("Received NATS message: " + msg.toString());
		
		Exchange exchange = natsConsumer.getEndpoint().createExchange();
        exchange.getIn().setBody(msg);
        exchange.getIn().setHeader(NatsConstants.NATS_MESSAGE_TIMESTAMP, System.currentTimeMillis());
        exchange.getIn().setHeader(NatsConstants.NATS_SUBSCRIPTION_ID, sid);
        try {
       	 natsConsumer.getProcessor().process(exchange);
        } catch (Exception e) {
       	 natsConsumer.getExceptionHandler().handleException("Error during processing", exchange, e);
        }	
	}

	void onClose(ConnectionEvent event) {
		
		if(adapterType == AdapterType.PRODUCER){	    	     
			if(natsProducer.getShutdownLatch() != null)
				natsProducer.getShutdownLatch().countDown();
		 }
		else if(adapterType == AdapterType.CONSUMER){	    	     
			if(natsConsumer.getShutdownLatch() != null)
				natsConsumer.getShutdownLatch().countDown();
		 }			
	}

	public void onShutdown() {	
		
	     if(natsConnector == null)
	    	 return;
	     
	     if(adapterType == AdapterType.CONSUMER){	
	    	 logger.info("Shutting down NatsConnector (Consumer)");
		     try {
		    	 natsConnector.unsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
		     } catch (Exception e) {
		    	 natsConsumer.getExceptionHandler().handleException("Error during unsubscribing", e);
		     }	
	     }
	     else if(adapterType == AdapterType.PRODUCER){	    	        
	    	 logger.info("Shutting down NatsConnector (Producer): ");
	     }
	}

	public void publish(String subject, String replySubject, byte[] payload) throws Exception {
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

	public void onReconnect(ConnectionEvent event) {
	 	logger.info("Adapter Reconnected ", event.toString());		
	}

	public void onException(NATSException ex) {
		logger.info("Adapter Exception ", ex.toString());	
		

		if(adapterType == AdapterType.PRODUCER){	    	     
			ex.printStackTrace();
		 }
		else if(adapterType == AdapterType.CONSUMER){	    	     
			natsConsumer.getExceptionHandler().handleException("Error during processing", ex);
		 }			

	}

	public void onDisconnect(ConnectionEvent event) {
		logger.info("Adapter Disconnected ", event.toString());		
	}

	public void shutdown() {
		this.natsConnector.shutdown();		
	}

	public Runnable getConnector() {		
		return natsConnector;
	}		
}

