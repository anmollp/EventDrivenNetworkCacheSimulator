import java.util.Comparator;
import java.util.PriorityQueue;

public class EventPriorityQueue {

    private static EventPriorityQueue instance;
    PriorityQueue<Event> pq;
    Comparator<Event> eventComparator = Comparator.comparingDouble(e -> e.key);

    private EventPriorityQueue() {
        pq = new PriorityQueue<>(eventComparator);
    }

    public static EventPriorityQueue getInstance() {
        if (instance == null) {
            instance = new EventPriorityQueue();
        }
        return instance;
    }

    public void enqueue(Event e) {
        pq.add(e);
    }

    public Event dequeue() throws Exception {
        try {
            return pq.remove();
        }
        catch(Exception ex) {
            throw new Exception("Event not in priority queue.");
        }
    }

    public boolean isEmpty() {
        return pq.isEmpty();
    }

}
