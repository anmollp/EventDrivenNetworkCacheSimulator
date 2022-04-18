public class FileMetadata {
    private double size;
    private double popularity;
    private double probability;

    public FileMetadata(double fileSize, double filePopularity, double probability) {
        this.size = fileSize;
        this.popularity = filePopularity;
        this.probability = probability;
    }

    public double getSize() {
        return this.size;
    }

    public double getPopularity() {
        return this.popularity;
    }

    public double getProbability() {
        return this.probability;
    }

}
