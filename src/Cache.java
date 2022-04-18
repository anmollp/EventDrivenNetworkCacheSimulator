public interface Cache {
    int capacity = 0;
    Node get(int fileId);
    void put(int fileId, FileMetadata fm);
    Object[] getCurrentCacheView();
}
