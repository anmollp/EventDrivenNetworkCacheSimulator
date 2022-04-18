import java.util.*;

public class LPCache implements  Cache{
    private final double capacity;
    private final Map<Integer, Node> cache;
    private final Queue<Node> minPopularityQueue;
    private final Queue<Node> maxPopularityQueue;
    Comparator<Node> minComparator = Comparator.comparingDouble(e -> e.getFilePopularity());
    Comparator<Node> maxComparator = Comparator.comparingDouble(e -> -1 * e.getFilePopularity());

    public LPCache(double capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.minPopularityQueue = new PriorityQueue<>(minComparator);
        this.maxPopularityQueue = new PriorityQueue<>(maxComparator);
    }

    public void insert(Node n) {
        this.cache.put(n.getFileId(), n);
        minPopularityQueue.add(n);
        maxPopularityQueue.add(n);
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
            Node leastPopularFile = minPopularityQueue.remove();
            maxPopularityQueue.remove(leastPopularFile);
            curSize -= leastPopularFile.getFileSize();
            this.cache.remove(leastPopularFile.getFileId());
        }
    }

    public Object[] getCurrentCacheView() {
        ArrayList<Double> popularity = new ArrayList<>();
        for(Node n: minPopularityQueue) {
            popularity.add(n.getFilePopularity());
        }
        return popularity.toArray();
    }
}
