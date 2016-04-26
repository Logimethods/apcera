package spark.stream.telematics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import scala.Tuple2;

@SuppressWarnings("serial")
public abstract class AverageSensorData {

	private static final Pattern SPACE = Pattern.compile(" "); 
	private static final Pattern EQ = Pattern.compile("=");

	public static class AvgCount implements Serializable {
		public AvgCount(int total, int num) {
			total_ = total;
			num_ = num; }
		public int total_;
		public int num_;
		public float avg() {
			return total_ / (float) num_; }
	}

	static Function<Integer, AvgCount> createAcc = new Function<Integer, AvgCount>() {
		public AvgCount call(Integer x) {
			return new AvgCount(x, 1);
		}
	};

	static Function2<AvgCount, Integer, AvgCount> addAndCount =
			new Function2<AvgCount, Integer, AvgCount>() {
		public AvgCount call(AvgCount a, Integer x) {
			a.total_ += x;
			a.num_ += 1;
			return a;
		}
	};

	static Function2<AvgCount, AvgCount, AvgCount> combine =
			new Function2<AvgCount, AvgCount, AvgCount>() {
		public AvgCount call(AvgCount a, AvgCount b) {
			a.total_ += b.total_;
			a.num_ += b.num_;
			return a;
		}
	};
	
	AvgCount initial = new AvgCount(0,0);

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
		// Get the lines, split them into words, count the words and print
		JavaDStream<String> messages = stackStream.map(
				new Function<Tuple2<String, String>, String>() {
					@Override
					public String call(Tuple2<String, String> tuple2) {
						System.out.println("tuple2:" + tuple2._2());
						System.out.println("tuple1:" + tuple2._1());
						return tuple2._2();
					}
				}
				);


		JavaDStream<String> words = messages.flatMap(new FlatMapFunction<String, String>() {
			@Override
			public Iterable<String> call(String x) {
				System.out.println("Message to split:" + x);
				//return Lists.newArrayList(SPACE.split(x));
				String[] strArray = SPACE.split(x);
				ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(strArray));
				Iterator<String> myIterator = arrayList.iterator();
				return IteratorUtils.toList(myIterator);
			}
		});


		JavaPairDStream<String, Integer> wordsPaired = words.mapToPair(
				new PairFunction<String, String, Integer>() {
					@Override
					public Tuple2<String, Integer> call(String s) {
						//Split string which is like: "Temperature=25"
						String[] measure = EQ.split(s);
						int value = Integer.parseInt(measure[1]);
						return new Tuple2<String, Integer>(measure[0], value);
					}
				});	    

		JavaPairDStream<String, AvgCount> avgCounts =
				wordsPaired.combineByKey(createAcc, addAndCount, combine, new HashPartitioner(3));
		return avgCounts;
	}

	/**
	 * @param ssc
	 * @return
	 */
	abstract protected JavaPairInputDStream<String, String> getStackStream(JavaStreamingContext ssc);
}

