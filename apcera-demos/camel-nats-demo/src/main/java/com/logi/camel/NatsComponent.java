package com.logi.camel;


import org.apache.camel.Exchange;


public class NatsComponent {
	
	NatsConnection natsConnection;
		
	public void process(Exchange exchange) throws Exception {
		
		String msg = exchange.getIn().getBody(String.class);		
		try{
	        natsConnection.connection.publish("MeterQueue", msg.getBytes());
	        System.err.printf("Published [%s] : '%s'\n", "MeterQueue", msg);
	    }
	    catch(Exception e){
	        e.printStackTrace();
	   }		
	}

	public NatsConnection getNatsConnection() {
		return natsConnection;
	}
	
	public void setNatsConnection(NatsConnection natsConnection) {
		this.natsConnection = natsConnection;
	}

}
