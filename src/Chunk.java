import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Chunk implements Serializable {
    String id;
    int size;
    int expectedReplicationDegree;
    AtomicInteger storedServers;

    Chunk(String id, int size, int expectedReplicationDegree) {
        this.id = id;
        this.size = size;
        this.expectedReplicationDegree = expectedReplicationDegree;
        storedServers = new AtomicInteger(1);
    }

    void save(String path) {
        try {
            FileOutputStream file = new FileOutputStream(path);
            ObjectOutputStream object = new ObjectOutputStream(file);
            object.writeObject(this);
            object.close();
        } catch (Exception e) {
        }
    }
}
