public class FileMetadata {
    private double size;
    private double popularity;

    public FileMetadata(double fileSize, double filePopularity) {
        this.size = fileSize;
        this.popularity = filePopularity;
    }

    public double getSize() {
        return this.size;
    }

    public double getPopularity() {
        return this.popularity;
    }

}
