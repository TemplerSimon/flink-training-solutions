package pl.simon.flink.longridealerts;

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide;

public class TaxiRideWithTimeStamp {



    private TaxiRide taxiRideStart;
    private TaxiRide taxiRideEnd;

    public TaxiRide getTaxiRideStart() {
        return taxiRideStart;
    }

    public void setTaxiRideStart(TaxiRide taxiRideStart) {
        this.taxiRideStart = taxiRideStart;
    }

    public TaxiRide getTaxiRideEnd() {
        return taxiRideEnd;
    }

    public void setTaxiRideEnd(TaxiRide taxiRideEnd) {
        this.taxiRideEnd = taxiRideEnd;
    }
}
