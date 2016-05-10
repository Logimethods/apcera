package spark.stream.telematics;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.script.ScriptException;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import nats.SendToNatsActionExecutionFunction;
import scala.Tuple2;

@SuppressWarnings("serial")
public abstract class AverageSensorData {

	public static final int OFF_LIMIT_VOLTAGE_COUNT = 3;
	public static final int OFF_LIMIT_VOLTAGE = 120;

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
		SparkConf sparkConf = getSparkConf();

		//Create the context with a 2 second batch size
		JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(2));

		JavaPairDStream<String, String> stackStream = getStackStream(ssc);  

		JavaPairDStream<String, AvgCount> avgCounts = computeAvgFromStream(stackStream);
		avgCountsExport(avgCounts);

		JavaPairDStream<String, Tuple2<Integer, Integer>> alerts = computeAlertFromStream(stackStream);
		alertsExport(alerts);

		ssc.start();
		ssc.awaitTermination();
	}

	/**
	 * @return
	 */
	protected SparkConf getSparkConf() {
		return new SparkConf().setAppName("JavaNetworkWordCount").setMaster("spark://192.168.1.1:7077");
		//SparkConf sparkConf = new SparkConf().setAppName("JavaNetworkWordCount").setMaster("local[2]");
	}

	/**
	 * @param avgCounts
	 */
	protected void avgCountsExport(JavaPairDStream<String, AvgCount> avgCounts) {
		avgCounts.print();
		avgCounts.foreachRDD(new SendToNatsActionExecutionFunction("192.168.0.107", 34686));
	}

	/**
	 * @param alerts
	 */
	protected void alertsExport(JavaPairDStream<String, Tuple2<Integer, Integer>> alerts) {
		alerts.print();
	}

	/**
	 * @param stackStream
	 * @return
	 */
	protected static JavaPairDStream<String, AvgCount> computeAvgFromStream(JavaPairDStream<String, String> stackStream) {
		JavaPairDStream<String, AvgCount> messages = stackStream.mapValues(
				new Function<String, AvgCount>() {
					@Override
					public AvgCount call(String val) {
						return new AvgCount(Integer.parseInt(val), 1);
					}
				}
				);

		JavaPairDStream<String, AvgCount> avgCounts = messages.reduceByKey(combine);
		
		return avgCounts;
	}

	/**
	 * @param stackStream
	 * @return
	 */
	protected static JavaPairDStream<String, Tuple2<Integer, Integer>> computeAlertFromStream(JavaPairDStream<String, String> stackStream) {
		JavaPairDStream<String, Integer> messages = stackStream.mapValues(
				new Function<String, Integer>() {
					@Override
					public Integer call(String val) {
						return Integer.parseInt(val);
					}
				}
				);

		JavaPairDStream<String, Integer> offLimit = messages.filter(
				new Function<Tuple2<String, Integer>, Boolean>() {
					@Override
					public Boolean call(Tuple2<String, Integer> tuple) {
						return tuple._2() >= OFF_LIMIT_VOLTAGE;
					}
				}
				);

		JavaPairDStream<String, Tuple2<Integer, Integer>> offLimitAccumulators = offLimit.mapValues(
				new Function<Integer, Tuple2<Integer, Integer>>() {
					@Override
					public Tuple2<Integer, Integer> call(Integer val) {
						return new Tuple2<Integer, Integer>(val, 1);
					}
				}
				);
		
		JavaPairDStream<String, Tuple2<Integer, Integer>> offLimitMax =
				offLimitAccumulators.reduceByKey(new Function2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>, Tuple2<Integer, Integer>>() {
					@Override
					public Tuple2<Integer, Integer> call(Tuple2<Integer, Integer> arg0, Tuple2<Integer, Integer> arg1)
							throws Exception {
						return new Tuple2<Integer, Integer>(Math.max(arg0._1(), arg1._1()), arg0._2() + arg1._2());
					}
					
				});
		
		
		JavaPairDStream<String, Tuple2<Integer, Integer>> alerts =
				offLimitMax.filter(
						new Function<Tuple2<String, Tuple2<Integer, Integer>>, Boolean>() {
							@Override
							public Boolean call(Tuple2<String, Tuple2<Integer, Integer>> tuple) throws Exception {
								return tuple._2()._2() >= OFF_LIMIT_VOLTAGE_COUNT;
							}
						}
						);
		
		return alerts;
	}

	// { "id" : "1", "voltage" : "104" }
	public final static Tuple2<String, String> jsonConvert(String json) throws IOException, ScriptException {
		Map<String, String> map = JSonHelper.parseJsonIntoMap(json);		
		String id = map.get("id");
		String voltage = map.get("voltage");
		return new Tuple2<String, String>(id, voltage);
	}

	/**
	 * @param ssc
	 * @return
	 */
	protected JavaPairDStream<String, String> getStackStream(JavaStreamingContext ssc) {
		final JavaReceiverInputDStream<String> rawMessages = getRawStackStream(ssc);
		final JavaPairDStream<String, String> messages = convertInputDStreamIntoPairDStream(rawMessages);		
		return messages;
	}

	/**
	 * @param rawMessages
	 * @return
	 */
	protected static JavaPairDStream<String, String> convertInputDStreamIntoPairDStream(
			final JavaReceiverInputDStream<String> rawMessages) {
		final JavaPairDStream<String, String> messages = rawMessages.mapToPair(
				new PairFunction<String, String, String>() {
					@Override
					public Tuple2<String, String> call(String json) throws Exception {
						return jsonConvert(json);
					}
				});
		return messages;
	}
	
	abstract protected JavaReceiverInputDStream<String> getRawStackStream(JavaStreamingContext ssc);

}

