public class Packet {
    int id;
    FileMetadata fm;
    double requestCreationTime;
    double arriveAtQueueTime;

    public Packet(int id, double fileSize, double filePopularity, double probability, double requestCreationTime, double arriveAtQueueTime) {
        this.id = id;
        this.fm = new FileMetadata(fileSize, filePopularity, probability);
        this.requestCreationTime = requestCreationTime;
        this.arriveAtQueueTime = arriveAtQueueTime;
    }

    public int getPacketId() {
        return id;
    }

    public double getRequestCreationTime() { return requestCreationTime; }

    public double getArriveAtQueueTime() { return  arriveAtQueueTime; }

    public FileMetadata metadata() {
        return fm;
    }

    public void setArriveAtQueueTime(double time) {
        arriveAtQueueTime = time;
    }
}
