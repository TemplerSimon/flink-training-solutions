package org.apache.flink.quickstart;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleFlinkProgram {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting simple flink program");

        final StreamExecutionEnvironment streamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();


        List<Integer> integerList = Arrays.asList(1, 2, 3, 4);

        DataStream<Integer> stream = streamExecutionEnvironment.fromCollection(integerList);

        stream.map(e -> {

            return e;
        }).filter(new FilterFunction<Integer>() {
            @Override
            public boolean filter(Integer integer) throws Exception {

                if (integer == null) {
                    System.out.println("Flink return null from map function");
                }

                return true;
            }
        }).print();


        streamExecutionEnvironment.execute();


    }
}
