package com.logi.camel;

import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class InputMessageConverter {
	
	private static final Logger LOG = LoggerFactory.getLogger(InputMessageConverter.class);
	
	private static final Pattern SPACE = Pattern.compile(" "); 
	private static final Pattern EQ = Pattern.compile("=");
	 


	public String convert(Exchange exchange) throws Exception {
		
		JsonBodyPojo pojoMessage = new JsonBodyPojo();
		Object objMessage = exchange.getIn().getBody();
		String  strMessage =  objMessage.toString();
		//strMessage = strMessage.replaceAll("\\n", "");
		String[] strArray = SPACE.split(strMessage);
		pojoMessage.id = EQ.split(strArray[1])[1];
		pojoMessage.voltage = EQ.split(strArray[0])[1];
		
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(pojoMessage);
				
		return json;	
	}

}


