import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Enumeration;

/**
 *  Class that represents a backed up file
 */
public class BackupFile implements Serializable {

    /**
     * Name of the file
     */
    String name;

    /**
     * Hash that represents the identified of the file
     */
    String fileID;

    /**
     * Desired replication degree of the file
     */
    int replicationDegree;

    /**
     * Maps chunk id to the id of the Peer which has saved it
     */
    ConcurrentHashMap<Integer, ConcurrentSkipListSet<Integer>> chunks;

    /**
     * Constructor for the class BackupFile
     * @param name
     *          Name of the backed up file
     * @param fileId
     *          Hash that represents the identifier of the file
     * @param replicationDegree
     *          Replication degree of the file
     */
    BackupFile(String name, String fileId, int replicationDegree) {
        this.name = name;
        this.fileID = fileId;
        this.replicationDegree = replicationDegree;
        chunks = new ConcurrentHashMap<>();
    }

    /**
     * Create an object of BackupFile from a serializable file
     * @param path
     *          Path of the serializable file
     * @return
     *          Object of BackupFile with the information of the serializable file
     */
    static BackupFile loadBackupFile(String path) {
        try {
            FileInputStream file = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(file);
            BackupFile backup = (BackupFile) in.readObject();
            in.close();
            file.close();
            return backup;
        } catch (Exception e) {
            return null;
        }       
    }

    /**
     * Saves the information of the object of the class BackupFile
     * @param path
     *          Path where the output file should be saved
     */
    void save(String path)
    {
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
     * Create a string with all the information of the object BackupFile
     */
	public String toString() {
		String res = "";

		res += "\tName: " + name + "\n\tFileID: " + fileID + "\n\tReplication Degree: " + replicationDegree + "\n\tChunks:\n";

		Enumeration<Integer> keys = chunks.keys();
		Integer key;

		while(keys.hasMoreElements())
		{
			key = keys.nextElement();

			res += "\t\t" + "ID: " + key + "\n\t\tPerceived Replication Degree: " + chunks.get(key).size() + "\n";
		}	

		return res;
	}
}
