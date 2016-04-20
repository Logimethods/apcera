package com.logi.camel;


import org.apache.camel.spring.Main;



public class CamelSpringStarter extends Main{
	
	
	public static  void main(String args) throws Exception {
		
		
		CamelSpringStarter main = new CamelSpringStarter();
		
		main.setApplicationContextUri("processorApplicationContext.xml");
	
		String[] argsL = new String[1];
		argsL[0] = args;
		main.run(argsL);
		
		
	}
}
