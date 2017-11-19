package pl.simon.flink.popularplaces;

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide;
import com.dataartisans.flinktraining.exercises.datastream_java.sources.TaxiRideSource;
import com.dataartisans.flinktraining.exercises.datastream_java.utils.GeoUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

public class PopularPlaces {

    static Logger logger = LoggerFactory.getLogger(PopularPlaces.class);

    final static int MAX_DELAY = 10;

    // File with test data
    final static String input = "/home/simon/Workspaces/flinktraining/source_data/nycTaxiRides.gz";
    final static int popThreshold = 29;        // threshold for popular places
    final static int maxEventDelay = 60;       // events are out of order by max 60 seconds
    final static int servingSpeedFactor = 600; // events of 10 minutes are served in 1 second


    public static void main(String[] args) throws Exception {
        logger.info("Starting program - Popular Places");


        // get an ExecutionEnvironment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // configure event-time processing
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // get the taxi ride data stream
        DataStream<TaxiRide> rides = env.addSource(new TaxiRideSource(input, maxEventDelay, servingSpeedFactor));


        DataStream<Tuple5<Float, Float, Long, Boolean, Integer>> popularPlaces = rides
                .filter(e -> GeoUtils.isInNYC(e.startLon, e.startLat) && GeoUtils.isInNYC(e.endLon, e.endLat))
                .map(new MyMapFunction())
                .keyBy(new MyKeySelector())
                .timeWindow(Time.minutes(15), Time.minutes(5))
                .apply(new MyWindowFunction())
                .filter(e -> e.f4 > popThreshold);

        popularPlaces.print();

        env.execute("PopularPlaces");

    }


}

/**
 * Map window function
 */
class MyWindowFunction implements WindowFunction<TaxiRidePopularPlaces, Tuple5<Float, Float, Long, Boolean, Integer>, Tuple2<Integer, Boolean>, TimeWindow> {
    @Override
    public void apply(Tuple2<Integer, Boolean> integerBooleanTuple2, TimeWindow timeWindow, Iterable<TaxiRidePopularPlaces> iterable, Collector<Tuple5<Float, Float, Long, Boolean, Integer>> collector) throws Exception {

        int count = 0;

        for (TaxiRidePopularPlaces taxiRidePopularPlaces : iterable) {
            count++;
        }

        float latCellId = GeoUtils.getGridCellCenterLat(integerBooleanTuple2._1);
        float lonCellId = GeoUtils.getGridCellCenterLon(integerBooleanTuple2._1);
        long timestamp = timeWindow.getEnd();

        collector.collect(new Tuple5<>(lonCellId, latCellId, timestamp, integerBooleanTuple2._2, count));
    }
}

class MyKeySelector implements KeySelector<TaxiRidePopularPlaces, Tuple2<Integer, Boolean>> {

    @Override
    public Tuple2<Integer, Boolean> getKey(TaxiRidePopularPlaces taxiRidePopularPlaces) throws Exception {

        Integer cellId = new Integer(taxiRidePopularPlaces.getCellId());
        Boolean eventType = new Boolean(taxiRidePopularPlaces.getTaxiRide().isStart);// true - start; false - end

        Tuple2 key = new Tuple2(cellId, eventType);

        return key;
    }
}

class MyMapFunction implements MapFunction<TaxiRide, TaxiRidePopularPlaces> {
    @Override
    public TaxiRidePopularPlaces map(TaxiRide taxiRide) throws Exception {

        TaxiRidePopularPlaces taxiRidePopularPlaces = new TaxiRidePopularPlaces();

        if (taxiRide.isStart) {
            taxiRidePopularPlaces.setCellId(GeoUtils.mapToGridCell(taxiRide.startLon, taxiRide.startLat));
        } else {
            taxiRidePopularPlaces.setCellId(GeoUtils.mapToGridCell(taxiRide.endLon, taxiRide.endLat));
        }

        taxiRidePopularPlaces.setTaxiRide(taxiRide);


        return taxiRidePopularPlaces;
    }
}