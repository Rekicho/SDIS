import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Chunk {
    int id;
    Integer size;
    ConcurrentSkipListSet<Integer> storedServers;

    Chunk(int id, Integer size)
    {
        this.id = id;
        this.size = size;
        storedServers = new ConcurrentSkipListSet<>();
    }
}
