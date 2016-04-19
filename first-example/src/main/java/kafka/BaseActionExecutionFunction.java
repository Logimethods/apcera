package kafka;

/**
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.VoidFunction;

import scala.Tuple2;
import spark.stream.telematics.AverageSensorData.AvgCount;

import java.util.List;

public abstract class BaseActionExecutionFunction implements
        VoidFunction<JavaPairRDD<String, AvgCount>> {
		
    private static final long serialVersionUID = -7719763983201600088L;

    static final String TIMESTAMP_FIELD = "timestamp";

    @Override
    public void call(JavaPairRDD<String, AvgCount> rdd) throws Exception {


            process(rdd);
        
    
    }

    public abstract void process(JavaPairRDD<String, AvgCount> rdd) throws Exception;

    public abstract Boolean check() throws Exception;
}
