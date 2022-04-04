public class Packet {
    int id;
    FileMetadata fm;
    double requestTime;

    public Packet(int id, double fileSize, double filePopularity, double requestTime) {
        this.id = id;
        this.fm = new FileMetadata(fileSize, filePopularity);
        this.requestTime = requestTime;
    }

    public int getPacketId() {
        return id;
    }

    public double getRequestTime() { return requestTime; }

    public FileMetadata metadata() {
        return fm;
    }
}
