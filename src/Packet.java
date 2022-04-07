public class Packet {
    int id;
    FileMetadata fm;
    double requestCreationTime;

    public Packet(int id, double fileSize, double filePopularity, double requestCreationTime) {
        this.id = id;
        this.fm = new FileMetadata(fileSize, filePopularity);
        this.requestCreationTime = requestCreationTime;
    }

    public int getPacketId() {
        return id;
    }

    public double getRequestCreationTime() { return requestCreationTime; }

    public FileMetadata metadata() {
        return fm;
    }
}
