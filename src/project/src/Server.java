import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;

public class Server implements ServerRMI {
    private String version;
    private int id;

    private Server(String version, int id) {
        this.version = version;
        this.id = id;
    }

    private String header(String message_type, String fileId, int chunkNo, Integer replicationDeg) {
        return message_type + " " + version + " " + id + " " + fileId + " " + chunkNo + " " + (replicationDeg != null ? replicationDeg : "") + " \r\n";
    }

    private String generateId(File file) {
        try {
            Path path = Paths.get(file.getAbsolutePath());
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);


            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] info = (file.getName()+attr.creationTime()+attr.lastModifiedTime()+attr.size()).getBytes();
            return new String(digest.digest(info));
        } catch (Exception e) {
            return file.getName();
        }
    }

    public String backup(String request) {
        String[] args = request.trim().split(" ", 2);

        File backupfile;
        InputStream fileToBackup;

        try {
            backupfile = new File(args[0]);
            fileToBackup = new FileInputStream(backupfile);

        } catch (Exception e) {
            return "FILE_NOT_FOUND";
        }

        String fileId = generateId(backupfile);

        int count;
        int chunkNo = 0;
        byte[] buffer = new byte[64000];

        try {
            do {
                count = fileToBackup.read(buffer);
                System.out.println(header("PUTCHUNK", fileId,chunkNo,Integer.parseInt(args[1])));
                chunkNo++;
            } while (count == 64000);
        } catch (Exception e) {
            return "FILE I/O ERROR";
        }

        return "STORED";
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Server <protocol_version> <server_id> <remote_object_name>");
            System.exit(-1);
        }

        try {
            Server obj = new Server(args[0], Integer.parseInt(args[1]));
            ServerRMI stub = (ServerRMI) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[2], stub);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
