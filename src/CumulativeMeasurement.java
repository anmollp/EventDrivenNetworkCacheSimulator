public class CumulativeMeasurement {
    double cumulatedValue;
    double numPoints;

    public CumulativeMeasurement(double cumulatedValue, double numPoints) {
        this.cumulatedValue = cumulatedValue;
        this.numPoints = numPoints;
    }

    public double getCumulatedValue() {
        return cumulatedValue;
    }

    public double getNumPoints() {
        return numPoints;
    }

    public void addToCumulatedValue(double value) {
        cumulatedValue += value;
    }

    public void addToNumPoints(double value) {
        numPoints += value;
    }

}
