import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class InputReader {
    int totalRequests; // total number of requests to be simulated.
    int totalTime; // total amount of time the simulation needs to run for.
    double requestRate; // per second
    double systemLoad; // For stability, system load should be in (0, 1)
    double serverRate; // Each server rate is relative to the FIFO queue transmission rate,
    //with transmission rate normalized to 1
    Distribution workLoadDistribution;
    double paretoAlpha; // pareto parameter alpha, must be > 1
    String cacheType;
    double institutionBandwidth;
    double logNormalMean;
    double logNormalStd;
    int numFiles;
    double fifoBandWidth;
    double cacheSize;
    double meanFileSize;

    public InputReader(String filename) {
            File fileObj = new File(filename);
            readFile(fileObj);
    }

    private void readFile(File fileObj) {
        try {
            Scanner scanner = new Scanner(fileObj);
            while (scanner.hasNextLine()) {
                String[] data = scanner.nextLine().split(":");
                switch (data[0]) {
                    case "Total Requests":
                        this.totalRequests = Integer.parseInt(data[1].trim());
                        break;
                    case "Total Time":
                        this.totalTime = Integer.parseInt(data[1].trim());
                        break;
                    case "Request Rate":
                        this.requestRate = Double.parseDouble(data[1].trim());
                        break;
                    case "System Load":
                        this.systemLoad = Double.parseDouble(data[1].trim());
                        break;
                    case "Server Rate":
                        this.serverRate = Double.parseDouble(data[1].trim());
                        break;
                    case "Workload Distribution":
                        int wld = Integer.parseInt(data[1].trim());
                        if (wld == 1) {
                            this.workLoadDistribution = Distribution.EXPONENTIAL;
                        }
                        else if(wld == 2) {
                            this.workLoadDistribution = Distribution.PARETO;
                        }
                        break;
                    case "Pareto Alpha":
                        this.paretoAlpha = Double.parseDouble(data[1].trim());
                        break;
                    case "Cache Type":
                        this.cacheType = data[1].trim();
                        break;
                    case "Institution Bandwidth":
                        this.institutionBandwidth = Double.parseDouble(data[1].trim());
                        break;
                    case "Log Normal Mean(ms)":
                        this.logNormalMean = Double.parseDouble(data[1].trim());
                        break;
                    case "Log Normal Standard Deviation(ms)":
                        this.logNormalStd = Double.parseDouble(data[1].trim());
                        break;
                    case "Number of files":
                        this.numFiles = Integer.parseInt(data[1].trim());
                        break;
                    case "FIFO Bandwidth":
                        this.fifoBandWidth = Double.parseDouble(data[1].trim());
                        break;
                    case "Cache Size":
                        this.cacheSize = Double.parseDouble(data[1].trim());
                        break;
                    case "Mean File Size":
                        this.meanFileSize = Double.parseDouble(data[1].trim());
                        break;
                }
            }
        }
        catch(FileNotFoundException e) {
            System.out.println("No such file");
            e.printStackTrace();
        }
    }

    public double getWorkLoadMean() {
        return systemLoad / requestRate;
    }

    public Distribution getWorkLoadDistribution() {
        return workLoadDistribution;
    }

    public double getParetoAlpha() {
        return paretoAlpha;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public double getServerRate() {
        return serverRate;
    }

    public double getRequestRate() {
        return requestRate;
    }

    public String getCacheType() {
        return cacheType;
    }

    public double getInstitutionBandwidth() {
        return institutionBandwidth;
    }

    public double getLogNormalMean() {
        return logNormalMean;
    }

    public double getLogNormalStd() {
        return logNormalStd;
    }

    public int getNumberOfFiles() {
        return numFiles;
    }

    public double getFifoBandWidth() { return fifoBandWidth ; }

    public double getCacheSize() {
        return cacheSize;
    }

    public double getMeanFileSize() {
        return meanFileSize;
    }
}
