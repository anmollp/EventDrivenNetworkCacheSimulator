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
    private double filePopularityScale;
    private ParetoDistribution fileSizeDistribution;
    private ParetoDistribution filePopularityDistribution;
    private Map<Integer, FileMetadata> fileMap;

    public FileSelection(int numberOfFiles, double fileSizeDistShape, double meanFileSize, double filePopularityAlpha,
                         double filePopularityScale, RandomGenerator rng) {
        this.numberOfFiles = numberOfFiles;
        this.fileSizeDistShape = fileSizeDistShape;
        this.meanFileSize = meanFileSize;
        this.filePopularityAlpha = filePopularityAlpha;
        this.filePopularityScale = filePopularityScale;
        this.fileSizeDistScale = (this.fileSizeDistShape - 1.0) / this.fileSizeDistShape * this.meanFileSize;
        this.fileSizeDistribution = new ParetoDistribution(rng, this.fileSizeDistScale, this.fileSizeDistShape);
        this.filePopularityDistribution = new ParetoDistribution(rng, this.filePopularityScale, this.filePopularityAlpha);
    }

    public Map<Integer, FileMetadata> generateFiles() {
        fileMap = new HashMap<>();
        double[] randomFileSizes = this.fileSizeDistribution.sample(numberOfFiles);
        double[] popularityFactor = this.filePopularityDistribution.sample(numberOfFiles);
        double[] filePopularityProbability = calculatePopularityForEachFile(popularityFactor);
        for(int i = 1; i <= randomFileSizes.length; i++) {
            fileMap.put(i, new FileMetadata(randomFileSizes[i-1], popularityFactor[i-1], filePopularityProbability[i-1]));
        }

        return fileMap;
    }

    private double[] calculatePopularityForEachFile(double[] filePopFact) {
        double totalPopularity = Arrays.stream(filePopFact).sum();
        double[] filePopularityProbability = new double[filePopFact.length];
        for(int i = 0; i < filePopularityProbability.length; i++ ) {
            filePopularityProbability[i] = filePopFact[i] / totalPopularity;
        }
        return filePopularityProbability;
    }

}

