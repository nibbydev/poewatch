package MainPack.PricerClasses;

public class StatsObject {
    //  Name: StatsObject
    //  Date created: 28.11.2017
    //  Last modified: 29.11.2017
    //  Description: Contains median, mean, count values

    private int count = 0;
    private double mean = 0.0;
    private double median = 0.0;

    public StatsObject(int count, double mean, double median) {
        this.count = count;
        this.mean = mean;
        this.median = median;
    }

    @Override
    public String toString() {
        return count + "," + mean + "," + median;
    }

    public void fromString(String line) {
        count = Integer.parseInt(line.split(",")[0]);
        mean = Double.parseDouble(line.split(",")[1]);
        median = Double.parseDouble(line.split(",")[2]);
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

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
