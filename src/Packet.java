public class Packet {
    int id;
    double requestTime;
    double arrivalTimeToQueue;
    double workLoad;

    public Packet(int id, double requestTime, double arrivalTimeToQueue, double workLoad) {
        this.id = id;
        this.requestTime = requestTime;
        this.arrivalTimeToQueue = arrivalTimeToQueue;
        this.workLoad = workLoad;
    }

    public int getPacketId() {
        return id;
    }

    public double getWorkLoad() {
        return workLoad;
    }

    public double getRequestTime() {
        return requestTime;
    }

    public double getArrivalTimeToQueue() {
        return arrivalTimeToQueue;
    }

    public void setPacketId(int pId) {
        id = pId;
    }

    public void setWorkLoad(double wl) {
        workLoad = wl;
    }

    public void setRequestTime(double modifiedRequestTime) {
        requestTime = modifiedRequestTime;
    }

    public void setArrivalTimeToQueue(double time) {
        arrivalTimeToQueue = time;
    }
}
