package com.logi.camel;

import java.util.Properties;

import org.apache.camel.spring.Main;
import org.nats.client.Connection;


public class CamelSpringStarter extends Main{
	
	
	public static  void main(String[] args) throws Exception {
		
		
		CamelSpringStarter main = new CamelSpringStarter();
		
		main.setApplicationContextUri("processorApplicationContext.xml");
	
		main.run(args);
		
		
	}
}
