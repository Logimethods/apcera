package spark.stream

// @see http://apache-spark-user-list.1001560.n3.nabble.com/Creating-in-memory-JavaPairInputDStream-for-testing-td23956.html
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.dstream.ConstantInputDStream;

object Helpers { 
  def createJavaPairInputDStream(jssc: JavaStreamingContext, 
                                       pair: JavaRDD[(String, String)]) = { 
    new JavaPairInputDStream(new ConstantInputDStream(jssc.ssc, pair.rdd)) 
  } 
} 