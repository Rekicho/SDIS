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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server implements ServerRMI {
    private String version;
    int id;
    private int disk_space;
    int space_used = 0;

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

    ThreadPoolExecutor executor;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private Server(String version, int id, String mc_host, int mc_port, String mdb_host, int mdb_port, String mdr_host, int mdr_port) throws Exception {
        this.version = version;
        this.id = id;
        this.disk_space = 1000;
        this.mc_host = mc_host;
        this.mc_port = mc_port;
        this.mdb_host = mdb_host;
        this.mdb_port = mdb_port;
        this.mdr_host = mdr_host;
        this.mdr_port = mdr_port;    

        mc = new MulticastSocket(mc_port);
        mc.joinGroup(InetAddress.getByName(mc_host));

        mdb = new MulticastSocket(mdb_port);
        mdb.joinGroup(InetAddress.getByName(mdb_host));

        mdr = new MulticastSocket(mdr_port);
        mdr.joinGroup(InetAddress.getByName(mdr_host));

        backedupFiles = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();

        loadInfo();
        

        executor = (ThreadPoolExecutor) Executors.newScheduledThreadPool(10);
        executor.execute(new MCThread(this));
        executor.execute(new MDBThread(this));
        executor.execute(new MDRThread(this));
    }

    private void loadFileChunkInfo(String path, String fileName) {
        File folder = new File(path + "/" + fileName);
        for(File file : folder.listFiles()) {
            String name = file.getName();
			if(name.substring(name.length()-4).equals(".ser")) {
				Chunk chunk = Chunk.loadChunkFile(path + "/" + fileName + "/" + name);
				space_used += chunk.size;
                storedChunks.put(fileName + "_" + name.substring(3,name.length()-3), chunk);
            }
        }
    }

    private void loadInfo() {
        String backup_path = "peer" + id + "/backup";
        Path path = Paths.get("peer" + id);
        if(Files.exists(path)){
            File folder = new File(backup_path);
            for (File fileEntry : folder.listFiles()){
                if(fileEntry.isDirectory()){
                    loadFileChunkInfo(backup_path, fileEntry.getName());
                }
                else if(fileEntry.getName().substring(fileEntry.getName().length()-4).equals(".ser")) { 
                    backedupFiles.put(fileEntry.getName(), BackupFile.loadBackupFile(backup_path + "/" + fileEntry.getName()));
                }
            }    
        }
        else{
            new File("peer" + id).mkdirs();
            new File("peer" + id + "/backup").mkdirs();
            new File("peer" + id + "/restored").mkdirs();
        }
 
    }

    String header(String message_type, String fileId, Integer chunkNo, Integer replicationDeg) {
        return message_type + " " + version + " " + id + " " + fileId + " " + (chunkNo != null ? chunkNo.longValue() : "") + " " + (replicationDeg != null ? replicationDeg.byteValue() : "") + " \r\n\r\n";
	}
	
	private char[] hexString(byte[] info){
		char[] hexChars = new char[info.length * 2];
		for (int j = 0; j < info.length; j++) {
			int v = info[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return hexChars;
	}

    private String generateId(File file) {
        try {
            Path path = Paths.get(file.getAbsolutePath());
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);


            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] info = digest.digest((file.getName() + attr.creationTime() + attr.lastModifiedTime() + attr.size()).getBytes());

            return new String(hexString(info));
        } catch (Exception e) {
            return file.getName();
        }
    }

    public String backup(String request) {
		request = request.trim();
        System.out.println("[Peer " + this.id + "] BACKUP " + request);
        String[] args = request.split(" ", 2);

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
        backedupFiles.put(fileId,backupFile);
        backupFile.save("peer" + id + "/backup/" + fileId + ".ser");

        System.out.println("[Peer " + id + "] Sending file " + fileId);

        int count;
        int chunkNo = 0;
        byte[] buffer = new byte[64000];
        byte[] header;

        try {
            do {
                count = fileToBackup.read(buffer);
                int tries = 1;
                header = header("PUTCHUNK", fileId, chunkNo, Integer.parseInt(args[1])).getBytes();

                byte[] message = new byte[header.length + count];

                System.arraycopy(header, 0, message, 0, header.length);
                System.arraycopy(buffer, 0, message, header.length, count);

                DatagramPacket chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(mdb_host), mdb_port);

                backupFile.chunks.put(chunkNo,new ConcurrentSkipListSet<>());
                backupFile.save("peer" + id + "/backup/" + fileId + ".ser");
                
                do {
                    System.out.println("[Peer " + id + "] Send chunk " + chunkNo + " from " + fileId + "(try n " + tries + ")");
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
	
	public String delete(String request) {
		request = request.trim();
		System.out.println("[Peer " + this.id + "] DELETE " + request);

		File file;

        try {
            file = new File(request);

        } catch (Exception e) {
            return "FILE_NOT_FOUND";
        }

		String fileId = generateId(file);

		backedupFiles.remove(fileId);

		try {
            new File("peer" + id + "/backup/" + fileId + ".ser").delete();
        } catch (Exception e) {
        }

		try{
			byte[] header = header("DELETE", fileId, null, null).getBytes();
			DatagramPacket deletePacket = new DatagramPacket(header, header.length, InetAddress.getByName(mc_host), mc_port);
	
			System.out.println("[Peer " + id + "] Delete file " + fileId);
			mc.send(deletePacket);
		} catch(Exception e) {
			return "ERROR";
		}
	
		return "DELETED";
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

            System.out.println("[Peer " + args[1]  +"] Ready");
        } catch (Exception e) {
            System.err.println("[Peer " + args[1] + "] Exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
