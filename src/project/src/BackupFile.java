import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class BackupFile {
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
}
