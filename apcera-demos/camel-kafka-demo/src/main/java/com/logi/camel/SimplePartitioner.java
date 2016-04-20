package com.logi.camel;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

/**
 * Round robin partitioner using a simple thread safe AotmicInteger
 */
public class SimplePartitioner implements Partitioner {
    private static final Logger log = Logger.getLogger(SimplePartitioner.class);

    final AtomicInteger counter = new AtomicInteger(0);

    public SimplePartitioner(VerifiableProperties props) {
        log.info("Instatiated the Round Robin Partitioner class");
    }
    /**
     * Take key as value and return the partition number
     */
    public int partition(Object key, int partitions) {

	    int partitionId = counter.incrementAndGet() % partitions;
		if (counter.get() > 65536) {
			counter.set(0);
		}
		log.info("Send to Partition: " + partitionId);
		
		return partitionId; 
	}
}

