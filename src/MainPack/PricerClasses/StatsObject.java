package MainPack.PricerClasses;

public class StatsObject {
    private int count = 0;
    private double mean = 0.0;
    private double median = 0.0;

    public StatsObject(int count, double mean, double median){
        this.count = count;
        this.mean = mean;
        this.median = median;
    }

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public int getCount() {
        return count;
    }
}
