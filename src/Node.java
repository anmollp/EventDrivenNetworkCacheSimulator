public class Node {
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