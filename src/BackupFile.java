import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BackupFile implements Serializable {
    String name;
    String fileID;
    int replicationDegree;
    ConcurrentHashMap<Integer, ConcurrentSkipListSet<Integer>> chunks;

    BackupFile(String name, String fileId, int replicationDegree)
    {
        this.name = name;
        this.fileID = fileId;
        this.replicationDegree = replicationDegree;
        chunks = new ConcurrentHashMap<>();
    }

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
}
