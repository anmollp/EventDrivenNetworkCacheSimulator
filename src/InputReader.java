import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class InputReader {
    int totalRequests; // total number of requests to be simulated.
    int totalTime; // total amount of time the simulation needs to run for.
    double requestRate; // per second
    double paretoAlpha; // pareto parameter alpha, must be > 1
    String cacheType;
    double institutionBandwidth;
    double logNormalMean;
    double logNormalStd;
    int numFiles;
    double fifoBandWidth;
    double cacheSize;
    double paretoMean;

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
                    case "Pareto Alpha":
                        this.paretoAlpha = Double.parseDouble(data[1].trim());
                        break;
                    case "Cache Type":
                        this.cacheType = data[1].trim();
                        break;
                    case "Institution Bandwidth(Mbps)":
                        this.institutionBandwidth = Double.parseDouble(data[1].trim());
                        break;
                    case "Log Normal Mean(s)":
                        this.logNormalMean = Double.parseDouble(data[1].trim());
                        break;
                    case "Log Normal Standard Deviation(s)":
                        this.logNormalStd = Double.parseDouble(data[1].trim());
                        break;
                    case "Number of files":
                        this.numFiles = Integer.parseInt(data[1].trim());
                        break;
                    case "FIFO Bandwidth(Mbps)":
                        this.fifoBandWidth = Double.parseDouble(data[1].trim());
                        break;
                    case "Cache Size(MB)":
                        this.cacheSize = Double.parseDouble(data[1].trim());
                        break;
                    case "Pareto Mean(MB)":
                        this.paretoMean = Double.parseDouble(data[1].trim());
                        break;
                }
            }
        }
        catch(FileNotFoundException e) {
            System.out.println("No such file");
            e.printStackTrace();
        }
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

    public double getParetoMean() {
        return paretoMean;
    }
}
