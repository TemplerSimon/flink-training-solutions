package pl.simon.flink.popularplaces;

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide;

public class TaxiRidePopularPlaces {

    private int cellId;
    private TaxiRide taxiRide;

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public TaxiRide getTaxiRide() {
        return taxiRide;
    }

    public void setTaxiRide(TaxiRide taxiRide) {
        this.taxiRide = taxiRide;
    }
}
