package spark.stream.telematics;

import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import nats.NatsReceiver;

public class NatsAverageSensorData extends AverageSensorData {

	public static void main(String[] args) throws Exception{
		new NatsAverageSensorData().processStream();
	}

	/**
	 * @param ssc
	 * @return
	 */
	protected JavaReceiverInputDStream<String> getRawStackStream(JavaStreamingContext ssc) {
		final JavaReceiverInputDStream<String> messages = ssc.receiverStream(
	    		new NatsReceiver("localhost", 4222, "MeterQueue", "MyGroup"));
		return messages;
	}

}

