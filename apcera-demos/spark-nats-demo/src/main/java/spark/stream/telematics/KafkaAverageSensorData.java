package spark.stream.telematics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;

import kafka.serializer.StringDecoder;

public class KafkaAverageSensorData extends AverageSensorData {

	public static void main(String[] args) throws Exception{
		new KafkaAverageSensorData().processStream();
	}

	/**
	 * @param ssc
	 * @return
	 */
	protected JavaReceiverInputDStream<String> getRawStackStream(JavaStreamingContext ssc) {
		Map<String, String> kafkaParams = new HashMap<>();
		kafkaParams.put("metadata.broker.list", "192.168.0.120:9092,192.168.0.121:9092");
		//kafkaParams.put("metadata.broker.list", "192.168.0.120:9092");

		HashSet<String> topicsSet = new HashSet<>(Arrays.asList("replicated-devices"));

		JavaPairInputDStream<String, String> directKafkaStream = 
				KafkaUtils.createDirectStream(ssc, String.class, String.class, 
						StringDecoder.class, StringDecoder.class, kafkaParams, topicsSet);
		// TODO Transform JavaPairInputDStream into JavaReceiverInputDStream
		return null;
	}

}

