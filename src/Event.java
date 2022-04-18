enum EventType {
    FILE_RECEIVED_EVENT(1),
    NEW_REQUEST_EVENT(2),
    ARRIVE_AT_QUEUE_EVENT(3),
    DEPART_QUEUE_EVENT(4);


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
}