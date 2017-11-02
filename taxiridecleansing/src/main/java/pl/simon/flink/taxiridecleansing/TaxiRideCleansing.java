package pl.simon.flink.taxiridecleansing;

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide;
import com.dataartisans.flinktraining.exercises.datastream_java.sources.TaxiRideSource;
import com.dataartisans.flinktraining.exercises.datastream_java.utils.GeoUtils;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of program that realise Taxi Ride Cleansing exercise
 */
public class TaxiRideCleansing {

    static Logger logger = LoggerFactory.getLogger(TaxiRideCleansing.class);

    final static int MAX_DELAY = 10;

    public static void main(String[] args) throws Exception {
        logger.info("Starting");

        // get an ExecutionEnvironment
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();
        // configure event-time processing
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // get the taxi ride data stream
        DataStream<TaxiRide> rides = env.addSource(new TaxiRideSource("/home/simon/Workspaces/flinktraining/source_data/nycTaxiRides.gz", MAX_DELAY));

        DataStream<TaxiRide> nyRides = rides.filter(e -> GeoUtils.isInNYC(e.startLon, e.startLat) && GeoUtils.isInNYC(e.endLon, e.endLat));

        nyRides.print();

        env.execute(TaxiRideCleansing.class.getName());

    }

}
