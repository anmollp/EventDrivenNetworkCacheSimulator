import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.distribution.ParetoDistribution;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FileSelection {
    private int numberOfFiles;
    private double fileSizeDistShape;
    private double meanFileSize;//in MBs
    private double fileSizeDistScale;
    private double filePopularityAlpha;
    private double filePopularityShape;
    private ParetoDistribution fileSizeDistribution;
    private ParetoDistribution filePopularityDistribution;
    private Map<Integer, FileMetadata> fileMap;

    public FileSelection(int numberOfFiles, double fileSizeDistShape, double meanFileSize, double filePopularityAlpha,
                         double filePopularityShape, RandomGenerator rng) {
        this.numberOfFiles = numberOfFiles;
        this.fileSizeDistShape = fileSizeDistShape;
        this.meanFileSize = meanFileSize;
        this.filePopularityAlpha = filePopularityAlpha;
        this.filePopularityShape = filePopularityShape;
        this.fileSizeDistScale = (this.fileSizeDistShape - 1.0) / this.fileSizeDistShape * this.meanFileSize;
        this.fileSizeDistribution = new ParetoDistribution(rng, this.fileSizeDistScale, this.fileSizeDistShape);
        this.filePopularityDistribution = new ParetoDistribution(rng, this.filePopularityShape, this.filePopularityAlpha);
    }

    public Map<Integer, FileMetadata> generateFiles() {
        fileMap = new HashMap<>();
        double[] randomFileSizes = this.fileSizeDistribution.sample(numberOfFiles);
        double[] popularityFactor = this.filePopularityDistribution.sample(numberOfFiles);
        double[] filePopularity = calculatePopularityForEachFile(popularityFactor);
        for(int i = 1; i <= randomFileSizes.length; i++) {
            fileMap.put(i, new FileMetadata(randomFileSizes[i-1],filePopularity[i-1]));
        }

        return fileMap;
    }

    private double[] calculatePopularityForEachFile(double[] filePopFact) {
        double totalPopularity = Arrays.stream(filePopFact).sum();
        double[] filePopularity = new double[filePopFact.length];
        for(int i = 0; i < filePopularity.length; i++ ) {
            filePopularity[i] = filePopFact[i] / totalPopularity;
        }
        return filePopularity;
    }




}

