import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Server implements ServerRMI {
    private String version;
    int id;
    private int disk_space;

    MulticastSocket mc;
    String mc_host;
    int mc_port;

    MulticastSocket mdb;
    String mdb_host;
    int mdb_port;

    MulticastSocket mdr;
    String mdr_host;
    int mdr_port;

    ConcurrentHashMap<String,BackupFile> backedupFiles;
    ConcurrentHashMap<String,Chunk> storedChunks;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private Server(String version, int id, String mc_host, int mc_port, String mdb_host, int mdb_port, String mdr_host, int mdr_port) throws Exception {
        this.version = version;
        this.id = id;
        this.disk_space = 1000;
        this.mc_host = mc_host;
        this.mc_port = mc_port;
        this.mdb_host = mdb_host;
        this.mdb_port = mdb_port;
        this.mdr_host = mc_host;
        this.mdr_port = mc_port;

        mc = new MulticastSocket(mc_port);
        mc.joinGroup(InetAddress.getByName(mc_host));

        mdb = new MulticastSocket(mdb_port);
        mdb.joinGroup(InetAddress.getByName(mdb_host));

        mdr = new MulticastSocket(mdr_port);
        mdr.joinGroup(InetAddress.getByName(mdr_host));

        backedupFiles = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();

        Thread mc_listen = new MCThread(this);
        Thread mdb_listen = new MDBThread(this);
        Thread mdr_listen = new MDRThread(this);

        mc_listen.start();
        mdb_listen.start();
        mdr_listen.start();
    }

    String header(String message_type, String fileId, long chunkNo, Integer replicationDeg) {
        return message_type + " " + version + " " + id + " " + fileId + " " + chunkNo + " " + (replicationDeg != null ? replicationDeg.byteValue() : "") + " \r\n\r\n";
    }

    private String generateId(File file) {
        try {
            Path path = Paths.get(file.getAbsolutePath());
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);


            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] info = digest.digest((file.getName() + attr.creationTime() + attr.lastModifiedTime() + attr.size()).getBytes());

            char[] hexChars = new char[info.length * 2];
            for (int j = 0; j < info.length; j++) {
                int v = info[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }

            return new String(hexChars);
        } catch (Exception e) {
            return file.getName();
        }
    }

    public String backup(String request) {
        String[] args = request.trim().split(" ", 2);

        File file;
        InputStream fileToBackup;

        try {
            file = new File(args[0]);
            fileToBackup = new FileInputStream(file);

        } catch (Exception e) {
            return "FILE_NOT_FOUND";
        }

        String fileId = generateId(file);

        BackupFile backupFile = new BackupFile(args[0],fileId,Integer.parseInt(args[1]));
        backedupFiles.put(args[0],backupFile);

        int count;
        int chunkNo = 0;
        byte[] buffer = new byte[64000];
        byte[] header;

        try {
            do {
                count = fileToBackup.read(buffer);
                int tries = 0;
                header = header("PUTCHUNK", fileId, chunkNo, Integer.parseInt(args[1])).getBytes();

                byte[] message = new byte[header.length + count];

                System.arraycopy(header, 0, message, 0, header.length);
                System.arraycopy(buffer, 0, message, header.length, count);

                DatagramPacket chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(mdb_host), mdb_port);

                backupFile.chunks.put(chunkNo,new ConcurrentSkipListSet<>());

                do {
                    mdb.send(chunkPacket);

                    Thread.sleep(tries * 1000);

                    if(backupFile.chunks.get(chunkNo).size() >= backupFile.replicationDegree)
                        break;

                    tries++;
                }
                while(tries != 5);

                chunkNo++;
            } while (count == 64000);
        } catch (Exception e) {
            return "FILE I/O ERROR";
        }

        return "STORED";
    }

    public static void main(String[] args) {
        if (args.length != 9) {
            System.out.println("Usage: java Server <protocol_version> <server_id> <remote_object_name> <MC_IP> <MC_port> <MDB_IP> <MDB_port> <MDR_IP> <MDR_port>");
            System.exit(-1);
        }

        try {
            Server obj = new Server(args[0], Integer.parseInt(args[1]), args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
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
