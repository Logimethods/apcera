package spark.stream.telematics;

import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

public class NatsAverageSensorData extends AverageSensorData {

	public static void main(String[] args) throws Exception{
		new NatsAverageSensorData().processStream();
	}

	/**
	 * @param ssc
	 * @return
	 */
	protected JavaPairInputDStream<String, String> getStackStream(JavaStreamingContext ssc) {
		return null;
	}

}

