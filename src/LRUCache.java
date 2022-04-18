import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LRUCache implements Cache{
    private final double capacity;
    private Map<Integer, Node> cache;
    private final Node left;
    private final Node right;

    public LRUCache(double capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.left = new Node(0, new FileMetadata(0, 0, 0));
        this.right = new Node(0, new FileMetadata(0, 0, 0));
        this.left.next = this.right;
        this.right.prev = this.left;
    }

    public void remove(Node n) {
        Node prev = n.prev;
        Node next = n.next;
        prev.next = next;
        next.prev = prev;
    }

    public void insert(Node n) {
        Node prev = this.right.prev;
        Node next = this.right;
        prev.next = n;
        next.prev = n;
        n.prev = prev;
        n.next = next;
    }

    public Node get(int fileId) {
        if (this.cache.containsKey(fileId)) {
            remove(this.cache.get(fileId));
            insert(this.cache.get(fileId));
            return this.cache.get(fileId);
        }
        return new Node(-1, new FileMetadata(-1, -1, 0));
    }

    public double getCurrentCacheSize() {
        double curCacheSize = 0;
        for(Node n: this.cache.values()) {
            curCacheSize += n.getFileSize();
        }
        return curCacheSize;
    }

    public void put(int fileId, FileMetadata fm) {
        if(this.cache.containsKey(fileId)) {
            remove(this.cache.get(fileId));
        }
        this.cache.put(fileId, new Node(fileId, fm));
        insert(this.cache.get(fileId));

        // if exceeds capacity remove least recently used and delete from cache
        while (getCurrentCacheSize() > this.capacity) {
            Node leastRecentlyUsed = this.left.next;
            remove(leastRecentlyUsed);
            this.cache.remove(leastRecentlyUsed.getFileId());
        }
    }

    public Object[] getCurrentCacheView() {
        ArrayList<Double> size = new ArrayList<>();
        for(Node n: cache.values()) {
            size.add(n.getFilePopularity());
        }
        return size.toArray();
    }
}
