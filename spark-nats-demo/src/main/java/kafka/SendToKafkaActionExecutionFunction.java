package kafka;

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


import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import scala.Tuple2;
import spark.stream.telematics.AverageSensorData.AvgCount;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.VoidFunction;

public class SendToKafkaActionExecutionFunction extends BaseActionExecutionFunction {

    private static final long serialVersionUID = -1661238643911306344L;

    private Producer<String, String> producer;
    

    private final String kafkaQuorum;

    public SendToKafkaActionExecutionFunction(String kafkaQuorum) {
        this.kafkaQuorum = kafkaQuorum;
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
						//Send average measure as Kafka message
						String measureName = avgMeasure._1();
						String avgValue = Float.toString(avgMeasure._2().avg());
						System.out.println(measureName + ": " + avgValue);
						List<KeyedMessage<String, String>> kafkaMessages = new ArrayList<>();
						kafkaMessages.add(new KeyedMessage<String, String>("replicared-average", measureName, avgValue));

						getProducer().send(kafkaMessages);
						
					}
                }
            );
        
        
    }


	@Override
    public Boolean check() throws Exception {
        return null;
    }

   

    private Producer<String, String> getProducer() {
        if (producer == null) {
            Properties properties = new Properties();
            properties.put("serializer.class", "kafka.serializer.StringEncoder");
            properties.put("metadata.broker.list", kafkaQuorum);
            properties.put("producer.type", "async");         
            properties.put("key.serializer.class", "kafka.serializer.StringEncoder");
            properties.put("value.serializer.class", "kafka.serializer.StringEncoder");
            
            producer = new Producer<String, String>(new ProducerConfig(properties));
        }
        return producer;
    }
}
