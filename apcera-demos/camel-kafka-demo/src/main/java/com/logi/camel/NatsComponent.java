package com.logi.camel;


import org.apache.camel.Exchange;

public class NatsComponent {
	
	private org.nats.client.Connection natsConnection;
	
public void process(Exchange exchange) throws Exception {
	
	/*
		
		String msg = "Hello " + exchange.getIn().getBody(String.class);
		String subject = "hello";
		String test1 = System.getenv("CNTM_PROCESS_NAME");
		String test2 = System.getenv("CNTM_HOST_IP");
		String test3 = System.getenv("CNTM_PROCESS_NAME");
		String test4 = System.getenv("NATS_URI");
		
		System.out.println("test1");	
		System.out.println("test2");
		System.out.println("test3");
		System.out.println("test4");
		System.out.println(msg);
		natsConnection.publish(subject, msg);
		*/
	}

public org.nats.client.Connection getNatsConnection() {
	return natsConnection;
}

public void setNatsConnection(org.nats.client.Connection natsConnection) {
	this.natsConnection = natsConnection;
}

}
