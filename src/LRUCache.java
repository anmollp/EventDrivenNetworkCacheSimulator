import java.io.File;
import java.util.HashMap;
import java.util.Map;

class Node {
    public int fileId;
    public FileMetadata fm;
    public Node prev;
    public Node next;
    public Node(int fileId, FileMetadata fm) {
        this.fileId = fileId;
        this.fm = fm;
        this.prev = null;
        this.next = null;
    }

    public int getFileId() {
        return fileId;
    }

    public double getFileSize() {
        return fm.getSize();
    }

    public double getFilePopularity() {
        return fm.getPopularity();
    }
}

public class LRUCache {
    private int capacity;
    private Map<Integer, Node> cache;
    private Node left;
    private Node right;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.left = new Node(0, new FileMetadata(0, 0));
        this.right = new Node(0, new FileMetadata(0, 0));
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
        return new Node(-1, new FileMetadata(-1, -1));
    }

    public void put(int fileId, FileMetadata fm) {
        if(this.cache.containsKey(fileId)) {
            remove(this.cache.get(fileId));
        }
        this.cache.put(fileId, new Node(fileId, fm));
        insert(this.cache.get(fileId));

        // if exceeds capacity remove least recently used and delete from cache
        if (this.cache.size() > this.capacity) {
            Node leastRecentlyUsed = this.left.next;
            remove(leastRecentlyUsed);
            this.cache.remove(leastRecentlyUsed.getFileId());
        }
    }
}
