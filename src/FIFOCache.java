import java.util.*;

public class FIFOCache implements  Cache{
    private final double capacity;
    private Map<Integer, Node> cache;
    private Queue<Node> fifoQueue;

    public FIFOCache(double capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.fifoQueue = new LinkedList<>();
    }

    public void insert(Node n) {
        this.cache.put(n.getFileId(), n);
        fifoQueue.add(n);
    }

    public double getCurrentCacheSize() {
        double curCacheSize = 0;
        for(Node n: this.cache.values()) {
            curCacheSize += n.getFileSize();
        }
        return curCacheSize;
    }


    @Override
    public Node get(int fileId) {
        if (this.cache.containsKey(fileId)) {
            return this.cache.get(fileId);
        }
        return new Node(-1, new FileMetadata(-1, -1, 0));
    }

    @Override
    public void put(int fileId, FileMetadata fm) {
        if(!cache.containsKey(fileId)) {
            insert(new Node(fileId, fm));
        }

        double curSize = getCurrentCacheSize();
        while(curSize > this.capacity) {
            Node firstOut = fifoQueue.remove();
            curSize -= firstOut.getFileSize();
            this.cache.remove(firstOut.getFileId());
        }
    }

    public Object[] getCurrentCacheView() {
        ArrayList<Double> popularity = new ArrayList<>();
        for(Node n: fifoQueue) {
            popularity.add(n.getFilePopularity());
        }
        return popularity.toArray();
    }
}
