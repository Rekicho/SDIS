import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
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
    private MulticastSocket mdb;
    private String mdb_host;
    private int mdb_port;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static final int TTL = 1;

    private Server(String version, int id, String mdb_host, int mdb_port) throws Exception {
        this.version = version;
        this.id = id;
        this.mdb_host = mdb_host;
        this.mdb_port = mdb_port;

        mdb = new MulticastSocket(mdb_port);
        mdb.joinGroup(InetAddress.getByName(mdb_host));

        Thread mdb_listen = new Thread("MDBListen") {
            private void interpretMessage(byte[] buffer, int length) throws Exception{
                String[] message = new String(buffer, StandardCharsets.US_ASCII).split("\r\n\r\n");

                int body_length = length - message[0].length() - 4;

                String[] args = message[0].trim().split(" ");

                if(!args[0].equals("PUTCHUNK"))
                    return;

                PrintWriter writer = new PrintWriter(args[3] + "_" + args[4], "ASCII");
                writer.print(message[1].substring(0,body_length));
                writer.close();
            }

            public void run() {
                byte[] buffer = new byte[64100];

                while(true) {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        mdb.receive(receivePacket);
                        interpretMessage(buffer, receivePacket.getLength());
                    } catch (Exception e) {
                        return;
                    }
                }
            }
        };

        mdb_listen.start();
    }

    private String header(String message_type, String fileId, long chunkNo, Integer replicationDeg) {
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
        long chunkNo = 0;
        byte[] buffer = new byte[64000];
        byte[] header;

        try {
            do {
                count = fileToBackup.read(buffer);
                header = header("PUTCHUNK", fileId, chunkNo, Integer.parseInt(args[1])).getBytes();

                byte[] message = new byte[header.length + count];

                System.arraycopy(header, 0, message, 0, header.length);
                System.arraycopy(buffer, 0, message, header.length, count);

                DatagramPacket chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(mdb_host), mdb_port);
                mdb.setTimeToLive(TTL);
                mdb.send(chunkPacket);

                chunkNo++;
                Thread.sleep(1000);
            } while (count == 64000);
        } catch (Exception e) {
            return "FILE I/O ERROR";
        }

        return "STORED";
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: java Server <protocol_version> <server_id> <remote_object_name> <MDB_IP> <MDB_port>");
            System.exit(-1);
        }

        try {
            Server obj = new Server(args[0], Integer.parseInt(args[1]), args[3], Integer.parseInt(args[4]));
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
