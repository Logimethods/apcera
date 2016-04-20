package org.nats.client;

import java.util.Properties;


public class Pub {

	public static void main(String[] args) throws Exception {
		Connection conn = Connection.connect(new Properties());

		System.out.println("Publishing...");		
		conn.publish("hello", "world");
		System.out.println("done...");
		conn.close();
		System.exit(0);
	}
}
