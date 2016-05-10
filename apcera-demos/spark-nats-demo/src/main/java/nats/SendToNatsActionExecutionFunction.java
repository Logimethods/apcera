package nats;

/**
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import scala.Tuple2;
import spark.stream.telematics.AverageSensorData.AvgCount;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;

public class SendToNatsActionExecutionFunction extends BaseActionExecutionFunction {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7465544117379701276L;

	private static final Logger LOG = LoggerFactory.getLogger(SendToNatsActionExecutionFunction.class);
	private String host;
	private int port;

	
    public SendToNatsActionExecutionFunction(String host, int port) {
        this.host = host;
        this.port = port;		
    }

    @Override
    public void process(JavaPairRDD<String, AvgCount> rdd) throws Exception {
       
       
        rdd.foreach(
                new VoidFunction<Tuple2<String,AvgCount>>() {
                   
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void call(Tuple2<String, AvgCount> avgMeasure) throws Exception {
						//Send average measure as NATS message
						String natsMessage = avgMeasure._1() + ":" + Float.toString(avgMeasure._2().avg());
						System.out.println("Sending out to NATS: "  + natsMessage);
						
						//String url = System.getenv("NATS_URI");
						//String url = System.getenv("NATSSERVERINT_URI");
						String url ="nats://192.168.64.129:42382";		//192.168.64.129:42382								
						ConnectionFactory cf = new ConnectionFactory(url);
						Connection connection = null;
						try  {
							 connection = cf.createConnection();
							 connection.publish("SparkQueue", natsMessage.getBytes());
					         System.out.printf("Published [%s] : '%s'\n", "SparkQueue", natsMessage);
					         connection.close();  
					    }
					    catch(Exception e)
					    {
					    	e.printStackTrace();
					    }
						System.out.println("Sent to NATS Success: ");
												   
						
					}
                }
            );
        
        
    }


	@Override
    public Boolean check() throws Exception {
        return null;
    }

	}

