import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BackupFile implements Serializable {
    String pathname;
    String fileID;
    int replicationDegree;
    ConcurrentHashMap<Integer, ConcurrentSkipListSet<Integer>> chunks;

    BackupFile(String pathName, String fileId, int replicationDegree)
    {
        this.pathname = pathName;
        this.fileID = fileId;
        this.replicationDegree = replicationDegree;
        chunks = new ConcurrentHashMap<>();
    }

    void save(String path)
    {
        try {
            FileOutputStream file = new FileOutputStream(path);
            ObjectOutputStream object = new ObjectOutputStream(file);
            object.writeObject(this);
            object.close();
        } catch (Exception e) {
        }
    }
}
