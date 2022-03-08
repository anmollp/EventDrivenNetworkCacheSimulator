enum EventType {
    REQUEST_ARRIVAL_EVENT(1),
    END_OF_SERVICE_EVENT(2),
    END_OF_TRANSMISSION_EVENT(3);

    private final int value;
    EventType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

public class Event {
    int flag;
    double key;
    Packet packet;
    int packId;
    EventType func;

    public Event(int flag, double key, Packet packet, int packId, EventType func) {
        this.flag = flag;
        this.key = key;
        this.packet = packet;
        this.packId = packId;
        this.func = func;
    }

    public Packet getPacket() {
        return packet;
    }

    public void setKey(double modifiedKey) {
        key = modifiedKey;
    }

    public void setPacket(Packet p) {
        packet = p;
    }

    public void setPacketId(int pId) {
        packId = pId;
    }

    public void setFunc(EventType t) {
        func = t;
    }
}