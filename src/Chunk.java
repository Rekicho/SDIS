import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that represents a chunk of information of a file
 */
public class Chunk implements Serializable, Comparable<Chunk> {
    
    /**
     * Serial Version ID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Identifier of the Chunk
     */
    String id;

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
    Chunk(String id, int size, int expectedReplicationDegree) {
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
            e.printStackTrace();
        }
	}
    
    /**
     * Create a string with all the information of the object Chunk
     */
	public String toString() {
		return id + "\n\tSize: " + size + "\n\tPerceived Replication Degree: " + storedServers + "\n";
	}

    /**
     * Overload of the compare function based on the amount of redundant information of the chunk
     */
	public int compareTo(Chunk other) {
		int res = (other.size * (other.storedServers.get() - other.expectedReplicationDegree)) - (size * (storedServers.get() - expectedReplicationDegree));

		if(res == 0)
			return other.size - size;

		return res;
	}

    /**
     * Create the File Id from the id
     * @return
     *          Hash of the file id
     */
	public String getFileID() {
		String[] info = id.split("_",2);

		return info[0];
	}

    /**
     * Get the Chunk number from the id
     * @return
     *          Integer of the chunk
     */
	public int getChunkNo() {
		String[] info = id.split("_",2);

		return Integer.parseInt(info[1]);
	}
}
