package com.logi.camel;

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;

public class NatsConnection {
	
	public Connection connection = null;
	public NatsConnection (){
		ConnectionFactory cf = new ConnectionFactory("nats://localhost:4222");
		 try  {
			 connection = cf.createConnection();
	        }
	        catch(Exception e)
	        {
	        	e.printStackTrace();
	        }
	}

}
