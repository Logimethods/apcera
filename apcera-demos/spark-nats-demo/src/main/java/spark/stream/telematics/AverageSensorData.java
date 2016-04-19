package spark.stream.telematics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import scala.Tuple2;
import kafka.SendToKafkaActionExecutionFunction;
import kafka.serializer.StringDecoder;

import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.apache.spark.streaming.Durations;

//import com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class AverageSensorData {
  
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

public static void main(String[] args) throws Exception{

	    //Create the context with a 1 second batch size
	    SparkConf sparkConf = new SparkConf().setAppName("JavaNetworkWordCount").setMaster("spark://192.168.1.1:7077");
	    //SparkConf sparkConf = new SparkConf().setAppName("JavaNetworkWordCount").setMaster("local[2]");

	    JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(2));
	    
	    Map<String, String> kafkaParams = new HashMap<>();
	    kafkaParams.put("metadata.broker.list", "192.168.0.120:9092,192.168.0.121:9092");
	    //kafkaParams.put("metadata.broker.list", "192.168.0.120:9092");
	    
	    HashSet<String> topicsSet = new HashSet<>(Arrays.asList("replicated-devices"));

	    JavaPairInputDStream<String, String> directKafkaStream = 
	    	     KafkaUtils.createDirectStream(ssc, String.class, String.class, 
	    	    		 StringDecoder.class, StringDecoder.class, kafkaParams, topicsSet);  

	      
	    // Get the lines, split them into words, count the words and print
	    JavaDStream<String> messages = directKafkaStream.map(
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
	    
	    avgCounts.print();
	
	    //avgCounts.foreachRDD(new SendToKafkaActionExecutionFunction("192.168.0.120:9092,192.168.0.121:9092"));
	    //avgCounts.foreachRDD(new SendToKafkaActionExecutionFunction("192.168.0.120:9092"));
	    

	    ssc.start();
	    ssc.awaitTermination();
	    
	  }
	
  }

