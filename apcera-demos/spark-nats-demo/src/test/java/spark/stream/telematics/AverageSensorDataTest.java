/**
 * 
 */
package spark.stream.telematics;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.apache.spark.Accumulator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.junit.Test;

import scala.Tuple2;
import spark.stream.Helpers;
import spark.stream.telematics.AverageSensorData.AvgCount;

/**
 * @author laugimethods
 *
 */
public class AverageSensorDataTest {

	/**
	 * Test method for {@link spark.stream.telematics.AverageSensorData#computeAvgFromStream(org.apache.spark.streaming.api.java.JavaPairInputDStream)}.
	 * @throws InterruptedException 
	 */
	@Test
	public void testComputeAvgFromStream() throws InterruptedException {
//		SparkConf sparkConf = new SparkConf().setAppName("My Spark Job").setMaster("local");
		JavaStreamingContext ssc = new JavaStreamingContext("local[2]", "AverageSensorDataTest", new Duration(2000));
		try {
			// @see http://apache-spark-user-list.1001560.n3.nabble.com/Creating-in-memory-JavaPairInputDStream-for-testing-td23956.html
			List<Tuple2<String, String>> list = new LinkedList<Tuple2<String, String>>();
			Tuple2<String, String> tupple = new Tuple2<String, String>("1", "112");
			list.add(tupple);
			tupple = new Tuple2<String, String>("1", "115");
			list.add(tupple);
			tupple = new Tuple2<String, String>("2", "123");
			list.add(tupple);
			JavaRDD<Tuple2<String, String>> rdd = ssc.sparkContext().parallelize(list);
			JavaPairInputDStream<String, String> stackStream = Helpers.createJavaPairInputDStream(ssc, rdd);
		    
			JavaPairDStream<String, AvgCount> avgCounts = AverageSensorData.computeAvgFromStream(stackStream);

			avgCounts.print();
			
		    ssc.start();
		    Thread.sleep(5000);					
		    ssc.stop();
		} finally {
			ssc.stop();
		}
	}

}
