package pl.simon.flink.longridealerts;

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide;
import com.dataartisans.flinktraining.exercises.datastream_java.sources.CheckpointedTaxiRideSource;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class LongRideAlerts {

    static Logger logger = LoggerFactory.getLogger(LongRideAlerts.class);

    final static int servingSpeedFactor = 600;

    public static void main(String[] args) throws Exception {
        logger.info("Starting program");

        // set up streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // set up checkpointing
        env.setStateBackend(new FsStateBackend("file:///tmp/checkpoints"));
        env.enableCheckpointing(1000);
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(60, Time.of(10, TimeUnit.SECONDS)));

        DataStream<TaxiRide> rides = env.addSource(
                new CheckpointedTaxiRideSource("/home/simon/Workspaces/flinktraining/source_data/nycTaxiRides.gz"
                        , servingSpeedFactor));

        DataStream<TaxiRide> ridesNotEnded = rides
                .keyBy(taxiRide -> taxiRide.rideId)
                .process(new LongRideProcessFunction());


        ridesNotEnded.print();
        env.execute(LongRideAlerts.class.getName());

    }

}

class LongRideProcessFunction extends ProcessFunction<TaxiRide, TaxiRide> {

    private long TWO_HOURS_LONG = 2 * 60 * 60 * 1000L + 1000L;//Plus one second

    /**
     * The state that is maintained by this process function
     */
    private ValueState<TaxiRideWithTimeStamp> state;

    @Override
    public void open(Configuration parameters) throws Exception {
        state = getRuntimeContext().getState(new ValueStateDescriptor<>("myState", TaxiRideWithTimeStamp.class));


    }


    @Override
    public void processElement(TaxiRide taxiRide, Context context, Collector<TaxiRide> collector) throws Exception {

        // retrieve the current count
        TaxiRideWithTimeStamp current = state.value();
        if (current == null) {
            current = new TaxiRideWithTimeStamp();
        }

        if (taxiRide.isStart) {
            current.setTaxiRideStart(taxiRide);
        } else {
            if (current.getTaxiRideStart() != null) {
                current.setTaxiRideEnd(taxiRide);
            }
        }

        state.update(current);
//        collector.collect(taxiRide);
        context.timerService().registerEventTimeTimer(current.getTaxiRideStart().getEventTime() + TWO_HOURS_LONG);



    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<TaxiRide> out)
            throws Exception {

        // get the state for the key that scheduled the timer
        TaxiRideWithTimeStamp result = state.value();


        if (result.getTaxiRideStart() != null && result.getTaxiRideEnd() == null) {
            out.collect(result.getTaxiRideStart());
        }

    }
}
