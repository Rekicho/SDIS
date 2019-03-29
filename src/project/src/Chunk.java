import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Chunk {
    String id;
    int size;
    int expectedReplicationDegree;
    AtomicInteger storedServers;

    Chunk(String id, int size, int expectedReplicationDegree)
    {
        this.id = id;
        this.size = size;
        this.expectedReplicationDegree = expectedReplicationDegree;
        storedServers = new AtomicInteger();
    }
}
