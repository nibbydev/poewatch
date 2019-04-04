package poe.Managers.Price.Bundles;

public class ResultBundle {
    private IdBundle idBundle;
    private double mean, median, mode, min, max;
    private int accepted;

    public ResultBundle(IdBundle idBundle, double mean, double median, double mode, double min, double max, int accepted) {
        this.idBundle = idBundle;
        this.mean = mean;
        this.median = median;
        this.mode = mode;
        this.min = min;
        this.max = max;
        this.accepted = accepted;
    }

    public IdBundle getIdBundle() {
        return idBundle;
    }

    public void setIdBundle(IdBundle idBundle) {
        this.idBundle = idBundle;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getMedian() {
        return median;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public double getMode() {
        return mode;
    }

    public void setMode(double mode) {
        this.mode = mode;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public int getAccepted() {
        return accepted;
    }

    public void setAccepted(int accepted) {
        this.accepted = accepted;
    }
}
