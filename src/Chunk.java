import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that represents a chunk of information of a file
 */
public class Chunk implements Serializable {
    
    /**
     * Identifier of the Chunk (its order)
     */
    int id;

    /**
     * Size of the Chunk in Bytes
     */
    int size;

    /**
     * Expected Replication Degree of the Chunk
     */
    int expectedReplicationDegree;

    /**
     * Number of servers which have this chunk saved
     */
    AtomicInteger storedServers;

    /**
     * Constructor for the class Chunk
     * @param id
     *          Identifier of the Chunk
     * @param size
     *          Size of the Chunk
     * @param expectedReplicationDegree
     *          Expected Replication Degree of the Chunk
     */
    Chunk(int id, int size, int expectedReplicationDegree) {
        this.id = id;
        this.size = size;
        this.expectedReplicationDegree = expectedReplicationDegree;
        storedServers = new AtomicInteger(1);
    }

    /**
     * Create an object of Chunk from a seriazable file
     * @param path
     *          Path of the seriazable file
     * @return
     *          Object of Chunk with the information of the seriazable file
     */
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

    /**
     * Saves the information of the object of the class Chunk
     * @param path
     *          Path where the output file should be saved
     */
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
    
    /**
     * Create a string with all the information of the object Chunk
     */
	public String toString(){
		String res = "";

		res += id + "\n\tSize: " + size + "\n\tPerceived Replication Degree: " + storedServers + "\n";

		return res;
	}
}
