package nats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Properties;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;



public class NatsReceiver extends Receiver<String> {

	  String host = null;
	  int port = -1;
	  Connection conn;

	  public NatsReceiver(String host_ , int port_) {
	    super(StorageLevel.MEMORY_AND_DISK_2());
	    host = host_;
	    port = port_;
	  }

	  public void onStart() {
	    // Start the thread that receives data over a connection
	    //new Thread()  {
	    //  @Override public void run() {
	        try {
				receive();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    //  }
	    //}.start();
	  }

	  public void onStop() {
	    // There is nothing much to do as the thread calling receive()
	    // is designed to stop by itself if isStopped() returns false
	  }

	  /** Create a socket connection and receive data until receiver is stopped */
	  private void receive() throws Exception{
		  
		
		 conn = Connection.connect(new Properties());
			
		 System.out.println("Listening on : ");			
					
		 conn.subscribe("hello", new MsgHandler() {
					public void execute(String msg) {
						System.out.println("Received update : " + msg);
						store(msg);
						
					}
		 });

		  
		  /*
	    Socket socket = null;
	    String userInput = null;

	    try {
	      // connect to the server
	      socket = new Socket(host, port);

	      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	      // Until stopped or connection broken continue reading
	      while (!isStopped() && (userInput = reader.readLine()) != null) {
	        System.out.println("Received data '" + userInput + "'");
	        store(userInput);
	      }
	      reader.close();
	      socket.close();

	      // Restart in an attempt to connect again when server is active again
	      restart("Trying to connect again");
	    } catch(ConnectException ce) {
	      // restart if could not connect to server
	      restart("Could not connect", ce);
	    } catch(Throwable t) {
	      // restart if there is any other error
	      restart("Error receiving data", t);
	    }
	    
		Connection conn;
		try {
			conn = Connection.connect(new Properties());
		

			System.out.println("Listening on : ");
			
			
			conn.subscribe("hello", new MsgHandler() {
				public void execute(String msg) {
					System.out.println("Received update : " + msg);
					store(msg);
				}
			});
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	  }
	}
