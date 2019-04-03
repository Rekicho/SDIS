import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
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

    static Chunk loadChunkFile(String path) {
        try {
            FileInputStream file = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(file);

			Chunk chunk = (Chunk) in.readObject();
            in.close();
            file.close();

            return chunk;
        } catch (Exception e) {
            return null;
        }
    }

    void save(String path) {
        try {
            FileOutputStream file = new FileOutputStream(path);
            ObjectOutputStream object = new ObjectOutputStream(file);
            object.writeObject(this);
            object.close();
            file.close();
        } catch (Exception e) {
        }
    }
}
