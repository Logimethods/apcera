/**
 * 
 */
package spark.stream.telematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.script.ScriptException;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
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
	@SuppressWarnings("serial")
	@Test
	public void testComputeAvgFromStream() throws InterruptedException {
		SparkConf sparkConf = new SparkConf().setAppName("My Spark Job").setMaster("local[2]");
		JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, new Duration(2000));
		try {
			// @see http://apache-spark-user-list.1001560.n3.nabble.com/Creating-in-memory-JavaPairInputDStream-for-testing-td23956.html
			List<Tuple2<String, String>> list = new LinkedList<Tuple2<String, String>>();
			Tuple2<String, String> tupple;
			
			String[] array1 = {"112", "124", "115", "114"};
			for (String str: array1){
				tupple = new Tuple2<String, String>("1", str);
				list.add(tupple);
			}

			String[] array2 = {"112", "124", "125", "122", "121"};
			for (String str: array2){
				tupple = new Tuple2<String, String>("2", str);
				list.add(tupple);
			}

			JavaRDD<Tuple2<String, String>> rdd = ssc.sparkContext().parallelize(list);
			JavaPairInputDStream<String, String> stackStream = Helpers.createJavaPairInputDStream(ssc, rdd);
		    
			JavaPairDStream<String, AvgCount> avgCounts = AverageSensorData.computeAvgFromStream(stackStream);
			avgCounts.print();
			
			avgCounts.foreachRDD(new VoidFunction<JavaPairRDD<String, AvgCount>>() {
				@Override
				public void call(JavaPairRDD<String, AvgCount> rdd) throws Exception {
					final List<Tuple2<String, AvgCount>> avgList = rdd.collect();
					assertEquals(2, avgList.size());
					Tuple2<String, AvgCount> avgA = avgList.get(0);
					Tuple2<String, AvgCount> avgB = avgList.get(1);
					Tuple2<String, AvgCount> avg1, avg2;
					if (avgA._1().equals("1")){
						avg1 = avgA;
						avg2 = avgB;
					} else {
						avg1 = avgB;
						avg2 = avgA;						
					}
					assertEquals(array1.length, avg1._2().num_);
					assertEquals(array2.length, avg2._2().num_);
					
					assertEquals(112 + 124 + 115 +114, avg1._2().total_);
				}				
			});
			
			JavaPairDStream<String, Tuple2<Integer, Integer>> alerts = AverageSensorData.computeAlertFromStream(stackStream);
			alerts.print();
			
			alerts.foreachRDD(new VoidFunction<JavaPairRDD<String, Tuple2<Integer, Integer>>>() {
				@Override
				public void call(JavaPairRDD<String, Tuple2<Integer, Integer>> rdd) throws Exception {
					List<Tuple2<String, Tuple2<Integer, Integer>>> alertsList = rdd.collect();
					assertEquals(1, alertsList.size());
					Tuple2<String, Tuple2<Integer, Integer>> alert = alertsList.get(0);
					// (2,(125,4))
					String id = alert._1();
					final Integer maximum = alert._2()._1();
					final Integer nb = alert._2()._2();
					assertEquals("2", id);
					assertTrue("Max: " + maximum, 125 == maximum);
					assertTrue("Nb: " + nb, 4 == nb);
				}				
			});
			
		    ssc.start();
		    Thread.sleep(3000);					
		} finally {
			ssc.stop();
		}
	}
	
	@Test
	public void testJsonConvert() throws IOException, ScriptException {
		String json = "{ \"id\" : \"1\", \"voltage\" : \"104\" }";
		Tuple2<String, String> tuple = AverageSensorData.jsonConvert(json);
		assertEquals("1", tuple._1());
		assertEquals("104", tuple._2());
	}
}
