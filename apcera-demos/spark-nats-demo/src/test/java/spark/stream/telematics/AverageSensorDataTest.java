/**
 * 
 */
package spark.stream.telematics;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.junit.Before;
import org.junit.Test;

import scala.Tuple2;
import spark.stream.Helpers;

/**
 * @author laugimethods
 *
 */
public class AverageSensorDataTest {
	
	JavaPairInputDStream<String, String> stackStream ;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		JavaStreamingContext ssc = new JavaStreamingContext(new SparkConf().setAppName("Spark Streaming Test"), new Duration(2));
		// @see http://apache-spark-user-list.1001560.n3.nabble.com/Creating-in-memory-JavaPairInputDStream-for-testing-td23956.html
		Tuple2<String, String> tupple = new Tuple2<String, String>("1", "112"); 
		List<Tuple2<String, String>> list = new LinkedList<Tuple2<String, String>>(); 
		list.add(tupple); 
		JavaRDD<Tuple2<String, String>> t = ssc.sparkContext().parallelize(list); 
		stackStream = Helpers.createJavaPairInputDStream(ssc, t); 
	}

	/**
	 * Test method for {@link spark.stream.telematics.AverageSensorData#computeAvgFromStream(org.apache.spark.streaming.api.java.JavaPairInputDStream)}.
	 */
	@Test
	public void testComputeAvgFromStream() {
		//fail("Not yet implemented");
	}

}
