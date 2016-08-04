package io.nats.connector;

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
	

	private NatsConnector dataFlowHandler;
	private Logger logger;	
	private NatsConsumer natsConsumer = null;
	private NatsProducer natsProducer = null;
	private Subscription sid;
	
	enum AdapterType{
		PRODUCER,
		CONSUMER
	};
	AdapterType pluginType;
	

	public CamelNatsAdapter(NatsConsumer natsConsumer) {
		this.natsConsumer = natsConsumer;
		this.pluginType = AdapterType.CONSUMER;
		
	}

	public CamelNatsAdapter(NatsProducer natsProducer) {
		this.natsProducer = natsProducer;
		this.pluginType = AdapterType.PRODUCER;
	}

//	@Override
		public boolean onStartup(Logger logger) {
			  this.logger = logger;
			  String name = Thread.currentThread().getName();
			  logger.info("Received NATS consumer onStartup event on thread:" + name);
		      return true;
		}

		public boolean onNatsInitialized(NatsConnector dataFlowHandler) {
		
			
			this.dataFlowHandler = dataFlowHandler;		
			
			if(pluginType == AdapterType.PRODUCER){
				logger.info("Received NATS producer onInitialized event");
				if (natsProducer.getStartupLatch() != null)
					natsProducer.getStartupLatch().countDown();
			}
			else if(pluginType == AdapterType.CONSUMER){
				logger.info("Received NATS consumer onInitialized event");
		        try
		        {
		        	if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getQueueName())) {
		        		dataFlowHandler.subscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
		        				natsConsumer.getEndpoint().getNatsConfiguration().getQueueName());
		        		if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages())) {
		        			dataFlowHandler.autoUnsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
		        					Integer.parseInt(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages()));
		        		}
		        	}
		        	else{
		        		dataFlowHandler.subscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
		        		if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages())) {
		        			dataFlowHandler.autoUnsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
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
			
			if(pluginType == AdapterType.PRODUCER){	    	     
				if(natsProducer.getShutdownLatch() != null)
					natsProducer.getShutdownLatch().countDown();
			 }
			else if(pluginType == AdapterType.CONSUMER){	    	     
				if(natsConsumer.getShutdownLatch() != null)
					natsConsumer.getShutdownLatch().countDown();
			 }
			
		}

//		@Override
		public void onShutdown() {	
			
		     if(dataFlowHandler == null)
		    	 return;
		     
		     String name = Thread.currentThread().getName();
		     
		     if(pluginType == AdapterType.CONSUMER){	
		    	 logger.info("Shutting down NatsConnector (Consumer)on thread: " + name);
			     try {
			    	 dataFlowHandler.unsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
			     } catch (Exception e) {
			    	 natsConsumer.getExceptionHandler().handleException("Error during unsubscribing", e);
			     }	
		     }
		     else if(pluginType == AdapterType.PRODUCER){	    	        
		    	 logger.info("Shutting down NatsConnector (Producer): ");
		     }
		}

//		@Override
		public void publish(String subject, String replySubject, byte[] payload) throws Exception {
			if (dataFlowHandler == null){
				logger.error("NATS connection is not initialized");
			    throw new Exception("Invalid State Nats Connector is null");
			}      
			
			Message m = new Message();
			m.setSubject(subject);
		        
			if (replySubject != null)
		        m.setReplyTo(replySubject);
		        
			m.setData(payload, 0, payload.length);
		
			dataFlowHandler.publish(m);
		
			try {
				dataFlowHandler.flush();
		    }
		    catch (Exception e){
		       	logger.error("Error with flush:  ", e);
		    }
			
		}

		public void onReconnect(ConnectionEvent event) {
			// TODO Auto-generated method stub
			
		}

		public void onException(NATSException ex) {
			// TODO Auto-generated method stub
			
		}

		public void onDisconnect(ConnectionEvent event) {
			// TODO Auto-generated method stub
			
		}
}

