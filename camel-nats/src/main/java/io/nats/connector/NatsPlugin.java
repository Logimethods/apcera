package io.nats.connector;

import org.apache.camel.Exchange;
import org.apache.camel.component.nats.NatsConstants;
import org.apache.camel.component.nats.NatsConsumer;
import org.apache.camel.component.nats.NatsProducer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;

import io.nats.client.ConnectionFactory;
import io.nats.client.Message;
import io.nats.client.Subscription;
import io.nats.connector.plugin.NATSConnector;
import io.nats.connector.plugin.NATSConnectorPlugin;
import io.nats.connector.plugin.NATSEvent;

public class NatsPlugin implements NATSConnectorPlugin{
	

	private NATSConnector natsConnector;
	private Logger logger;	
	private NatsConsumer natsConsumer = null;
	private NatsProducer natsProducer = null;
	private Subscription sid;
	private boolean subscribed = false;
	
	enum PluginType{
		PRODUCER,
		CONSUMER
	};
	PluginType pluginType;
	

	public NatsPlugin(NatsConsumer natsConsumer) {
		this.natsConsumer = natsConsumer;
		this.pluginType = PluginType.CONSUMER;
		
	}

	public NatsPlugin(NatsProducer natsProducer) {
		this.natsProducer = natsProducer;
		this.pluginType = PluginType.PRODUCER;
	}

	@Override
		public boolean onStartup(Logger logger, ConnectionFactory factory) {
			  this.logger = logger;
			  String name = Thread.currentThread().getName();
			  logger.info("Received NATS consumer onStartup event on thread:" + name);
		      return true;
		}

		@Override
		public boolean onNatsInitialized(NATSConnector connector) {
		
			
			this.natsConnector = connector;		
			
			if(pluginType == PluginType.PRODUCER){
				logger.info("Received NATS producer onInitialized event");
				if (natsProducer.getStartupLatch() != null)
					natsProducer.getStartupLatch().countDown();
			}
			else if(pluginType == PluginType.CONSUMER){
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
		        		subscribed = true;
		        	}
		        	else{
		        		connector.subscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
		        		if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages())) {
		        			natsConnector.autoUnsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
		        					Integer.parseInt(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages()));
		        		}
		        		subscribed = true;
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

		@Override
		public void onNATSMessage(Message msg) {
			
			String name = Thread.currentThread().getName();
			//logger.info("Received message on thread: " + name + " ; " + msg.toString());
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
	                onClose();
	                break;
	            default:
	                logger.info("NATS Event Unrecognized event: " + message);
	        }		
		}

		private void onClose() {
			
			if(pluginType == PluginType.PRODUCER){	    	     
				if(natsProducer.getShutdownLatch() != null)
					natsProducer.getShutdownLatch().countDown();
			 }
			else if(pluginType == PluginType.CONSUMER){	    	     
				if(natsConsumer.getShutdownLatch() != null)
					natsConsumer.getShutdownLatch().countDown();
			 }
			
		}

		@Override
		public void onShutdown() {	
			
		     if(natsConnector == null)
		    	 return;
		     
		     String name = Thread.currentThread().getName();
		     
		     if(pluginType == PluginType.CONSUMER){	
		    	 logger.info("Shutting down NatsConnector (Consumer)on thread: " + name);
			     try {
			    	 natsConnector.unsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
			     } catch (Exception e) {
			    	 natsConsumer.getExceptionHandler().handleException("Error during unsubscribing", e);
			     }	
		     }
		     else if(pluginType == PluginType.PRODUCER){	    	        
		    	 logger.info("Shutting down NatsConnector (Producer): ");
		     }
		}

		@Override
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
}

