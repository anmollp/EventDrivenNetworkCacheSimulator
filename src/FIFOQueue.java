import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

//Singleton class
public class FIFOQueue {

    private static FIFOQueue instance;
    public double workLoad = 0;
    Queue<Packet> fifoQueue;

    private FIFOQueue() {
        fifoQueue = new LinkedList<>();
    }

    public static FIFOQueue getInstance() {
        if(instance == null) {
            instance = new FIFOQueue();
        }
        return instance;
    }

    public int getTotalItems() {
        return fifoQueue.size();
    }

    public void enqueue(Packet p) {
        fifoQueue.add(p);
        workLoad += p.getWorkLoad();
    }

    public Packet getHead() {
        return fifoQueue.peek();
    }

    public void dequeue(Packet p) throws Exception {
        try {
            if (Objects.equals(fifoQueue.peek(), p)) {
                fifoQueue.remove();
                workLoad -= p.workLoad;
            }
            else {
                throw new Exception("Requested packet cannot be dequeued as it is not the head.");
            }
        }
        catch(Exception e) {
            throw  new Exception("Cannot remove items from empty queue: " + e.toString());
        }

    }
}
