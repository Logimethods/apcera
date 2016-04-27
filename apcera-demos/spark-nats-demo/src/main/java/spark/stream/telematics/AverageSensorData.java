package spark.stream.telematics;

import java.io.Serializable;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

@SuppressWarnings("serial")
public abstract class AverageSensorData {

	public static class AvgCount implements Serializable {
		public AvgCount(int total, int num) {
			total_ = total;
			num_ = num; }
		public int total_;
		public int num_;
		public float avg() {
			return total_ / (float) num_; }
		@Override
		public String toString() {
			return "AvgCount [total_=" + total_ + ", num_=" + num_ + ", avg()=" + avg() + "]";
		}
	}

	static Function2<AvgCount, AvgCount, AvgCount> combine =
			new Function2<AvgCount, AvgCount, AvgCount>() {
		public AvgCount call(AvgCount a, AvgCount b) {
			a.total_ += b.total_;
			a.num_ += b.num_;
			return a;
		}
	};

	/**
	 * 
	 */
	protected void processStream() {
		//Create the context with a 1 second batch size
		SparkConf sparkConf = new SparkConf().setAppName("JavaNetworkWordCount").setMaster("spark://192.168.1.1:7077");
		//SparkConf sparkConf = new SparkConf().setAppName("JavaNetworkWordCount").setMaster("local[2]");

		JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(2));

		JavaPairInputDStream<String, String> stackStream = getStackStream(ssc);  

		JavaPairDStream<String, AvgCount> avgCounts = computeAvgFromStream(stackStream);

		avgCounts.print();

		//avgCounts.foreachRDD(new SendToKafkaActionExecutionFunction("192.168.0.120:9092,192.168.0.121:9092"));
		//avgCounts.foreachRDD(new SendToKafkaActionExecutionFunction("192.168.0.120:9092"));

		ssc.start();
		ssc.awaitTermination();
	}

	/**
	 * @param stackStream
	 * @return
	 */
	protected static JavaPairDStream<String, AvgCount> computeAvgFromStream(JavaPairInputDStream<String, String> stackStream) {
		JavaPairDStream<String, AvgCount> messages = stackStream.mapValues(
				new Function<String, AvgCount>() {
					@Override
					public AvgCount call(String val) {
						System.out.println("tuple2:" + val);
						return new AvgCount(Integer.parseInt(val), 1);
					}
				}
				);

		JavaPairDStream<String, AvgCount> avgCounts = messages.reduceByKey(combine);
		
		return avgCounts;
	}

	/**
	 * @param ssc
	 * @return
	 */
	abstract protected JavaPairInputDStream<String, String> getStackStream(JavaStreamingContext ssc);
}

