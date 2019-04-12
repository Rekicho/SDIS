import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
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
import java.io.IOException;
import java.util.Enumeration;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Class that represents a Peer
 */
public class Peer implements PeerRMI {

    /**
     * Version of the Peer
     */
    String version;

    /**
     * Identifier of the Peer
     */
    int id;

    /**
     * Maximum disk space available for use
     */
    int disk_space;

    /**
     * Space ocuppied
     */
    AtomicInteger space_used;

    /**
     * Information of Control Channel
     */
    MulticastSocket mc;
    String mc_host;
    int mc_port;

    /**
     * Information of Data Backup Channel
     */
    MulticastSocket mdb;
    String mdb_host;
    int mdb_port;

    /**
     * Information of Data Recovery Channel
     */
    MulticastSocket mdr;
    String mdr_host;
    int mdr_port;

    /**
     * Information for the BackedupFiles by this Peer
     */
    ConcurrentHashMap<String, BackupFile> backedupFiles;

    ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>> deletedFiles;

    /**
     * Information for the stored chunks by this Peer
     */
    ConcurrentHashMap<String, Chunk> storedChunks;

    /**
     * Information about the restored files by this Peer
     */
	ConcurrentHashMap<String, RestoredFile> restoredFiles;
	
	ConcurrentHashMap<String, AtomicInteger> chunkTries;

	ConcurrentSkipListSet<String> restoredChunkMessages;

    /**
     * Thread Pool for all the threads of this Peer
     */
    ThreadPoolExecutor executor;

    AtomicInteger restoresCount;

    /**
     * Auxiliar char array for hexadecimal conversion
     */
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Constructor for an object of Peer
     * 
     * @param version  Version of the Peer
     * @param id       Identifier of the Peer
     * @param mc_host  Address of the Control Channel
     * @param mc_port  Port of the Control Channel
     * @param mdb_host Address of the Data Backup Channel
     * @param mdb_port Port of the Data Backup Channel
     * @param mdr_host Address of the Data Recovery Channel
     * @param mdr_port Port of the Data Recovery Channel
     * @throws Exception
     */
    private Peer(String version, int id, String mc_host, int mc_port, String mdb_host, int mdb_port, String mdr_host,
            int mdr_port) throws Exception {
        this.version = version;
        this.id = id;
		this.disk_space = 1000000000;
		this.space_used = new AtomicInteger(0);
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
        restoredFiles = new ConcurrentHashMap<>();
        deletedFiles = new ConcurrentHashMap<>();
		restoredChunkMessages = new ConcurrentSkipListSet<>();

        restoresCount = new AtomicInteger(0);
		
		if(!version.equals(Const.VERSION_1_0))
			chunkTries = new ConcurrentHashMap<>();

        loadInfo();

        executor = (ThreadPoolExecutor) Executors.newScheduledThreadPool(100);
        executor.execute(new MCThread(this));
        executor.execute(new MDBThread(this));
        executor.execute(new MDRThread(this));

        sendEntryMessage();
    }

    private void sendEntryMessage() {
        byte[] header = header("HELLO",id,null).getBytes(StandardCharsets.US_ASCII);
        DatagramPacket helloPacket;

        try {
            helloPacket = new DatagramPacket(header, header.length, InetAddress.getByName(mc_host), mc_port);

            System.out.println("[Peer " + id + "] Hello Network");
            mc.send(helloPacket);
        } catch (Exception e) {
            return ;
        }
    }

    /**
     * Load information of the chunks saved in the Peer from a file
     * 
     * @param path     Path of the file with the information
     * @param fileName Name of the file with the information
     */
    private void loadFileChunkInfo(String path, String fileName) {
        File folder = new File(path + "/" + fileName);
        for (File file : folder.listFiles()) {
            String name = file.getName();
            if (name.substring(name.length() - 4).equals(".ser")) {
                Chunk chunk = Chunk.loadChunkFile(path + "/" + fileName + "/" + name);
                space_used.set(space_used.get() + chunk.size);
                storedChunks.put(fileName + "_" + name.substring(3, name.length() - 4), chunk);
            }
        }
    }

    /**
     * Load information about the Peer
     */
    private void loadInfo() {
        String backup_path = "peer" + id + "/backup";
        Path path = Paths.get("peer" + id);
        if (Files.exists(path)) {
            File folder = new File(backup_path);
            for (File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    loadFileChunkInfo(backup_path, fileEntry.getName());
                } else if (fileEntry.getName().substring(fileEntry.getName().length() - 4).equals(".ser")) {
                    backedupFiles.put(fileEntry.getName().substring(0, fileEntry.getName().length() - 4),
                            BackupFile.loadBackupFile(backup_path + "/" + fileEntry.getName()));
                }
            }
        } else {
            new File("peer" + id).mkdirs();
            new File("peer" + id + "/backup").mkdirs();
            new File("peer" + id + "/restored").mkdirs();
        }

    }

    /**
     * Creates a String of a header formatted following the protocol rules
     * 
     * @param message_type   Type of the message
     * @param fileId         Identified of the file
     * @param chunkNo        Number of the chunk
     * @param replicationDeg Replication degree
     * @return Header String
     */
    String header(String message_type, String fileId, Integer chunkNo, Integer replicationDeg) {
        return message_type + " " + version + " " + id + " " + fileId + " "
                + (chunkNo != null ? chunkNo.longValue() : "") + " "
                + (replicationDeg != null ? replicationDeg.byteValue() : "") + " " + Const.CRLF;
    }

    
    String header(String message_type, Integer serverId, String fileId) {
        return message_type + " " + version + " " + serverId + " " + (fileId != null? fileId : "") + " " + Const.CRLF;
    }

    /**
     * Creates a String of a header for restore protocols with higher version
     * @param message_type
     *                  Type of the message
     * @param fileId
     *                  Identifier of the file
     * @param chunkNo
     *                  Number of the chunk
     * @param replicationDeg
     *                  Replication degree
     * @param ipAddress
     *                  Address to forward the message
     * @param Port
     *                  Port to forward the message
     * @return
     *                  Header String
     */
    String header(String message_type, String fileId, Integer chunkNo, Integer replicationDeg, String ipAddress, String port){
		return message_type + " " + version + " " + id + " " + fileId + " "
                + (chunkNo != null ? chunkNo.longValue() : "") + " "
                + (replicationDeg != null ? replicationDeg.byteValue() : "") + " " + Const.NEW_LINE + 
                ipAddress + " " + port + " " + Const.CRLF;
    }


    /**
     * Converter of a byte array into a char array
     * 
     * @param info Information to be converted
     * @return Char array
     */
    private char[] hexString(byte[] info) {
        char[] hexChars = new char[info.length * 2];
        for (int j = 0; j < info.length; j++) {
            int v = info[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return hexChars;
    }

    /**
     * Identifier generator
     * 
     * @param file File for the identifier to be created
     * @return String with the name of the file
     */
    private String generateId(File file) {
        try {
            Path path = Paths.get(file.getAbsolutePath());
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

            MessageDigest digest = MessageDigest.getInstance(Const.SHA256);
            byte[] info = digest
                    .digest((file.getName() + attr.creationTime() + attr.lastModifiedTime() + attr.size()).getBytes(StandardCharsets.US_ASCII));

            return new String(hexString(info));
        } catch (Exception e) {
            return file.getName();
        }
    }

    /**
     * Backup a file
     * 
     * @param request Request for the backup of a file
     * @return String with the information about the success or not of the function
     */
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
            return Error.FILE_NOT_FOUND;
        }

        String fileId = generateId(file);

        BackupFile backupFile = new BackupFile(file.getName(), fileId, Integer.parseInt(args[1]));
        backedupFiles.put(fileId, backupFile);
        backupFile.save("peer" + id + "/backup/" + fileId + ".ser");

        System.out.println("[Peer " + id + "] Sending file " + fileId);

        int count;
        int chunkNo = 0;
        byte[] buffer = new byte[Const.BUFFER_SIZE];
        byte[] header;
		ConcurrentHashMap<Integer, DatagramPacket> packets = new ConcurrentHashMap<>();

		try {
            do {
                count = fileToBackup.read(buffer);

                header = header(Const.MDB_PUTCHUNK, fileId, chunkNo, Integer.parseInt(args[1])).getBytes(StandardCharsets.US_ASCII);

                byte[] message = new byte[header.length + count];
                System.arraycopy(header, 0, message, 0, header.length);
                System.arraycopy(buffer, 0, message, header.length, count);

                packets.put(chunkNo,new DatagramPacket(message, message.length,
                        InetAddress.getByName(mdb_host), mdb_port));

                backupFile.chunks.put(chunkNo, new ConcurrentSkipListSet<>());
                backupFile.save("peer" + id + "/backup/" + fileId + ".ser");

                chunkNo++;
            } while (count == Const.BUFFER_SIZE);

            fileToBackup.close();
        } catch (Exception e) {
            return Error.FILE_IO;
        }

		AtomicInteger actualChunk = new AtomicInteger(0);

		for(int i = 0; i < packets.size(); i++)
			executor.execute(new BackupThread(this, packets, actualChunk, backupFile));

		return "File Successfully Backed Up";
    }

    /**
     * Send a restore message
     * @param backupFile
     *              
     * @param fileId
     * @return
     */
    private String sendRestoreMsg(BackupFile backupFile, String fileId) {
        ServerSocket restoreSocket = null;
        Socket connectionSocket = null;

        byte[] header;
        DatagramPacket restorePacket;
        String address = null;
        int restores = restoresCount.getAndIncrement();

        if(version.equals(Const.VERSION_1_1)) {
            try {
				restoreSocket = new ServerSocket(Const.TCP_BASE_PORT + restores);
				restoreSocket.setSoTimeout(Const.MEDIUM_DELAY);
            } catch (Exception e) {
                return Error.TCP_SERVER_SOCKET_CREATION;
            }

            try {
                address = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                return Error.FAILED_TO_READ_IP;
            }
            
        }

        for (int chunkNo = 0; chunkNo < backupFile.chunks.size(); chunkNo++) {
            if(version.equals(Const.VERSION_1_0))
                header = header(Const.MSG_GETCHUNK, fileId, chunkNo, null).getBytes(StandardCharsets.US_ASCII);
			else
				header = header(Const.MSG_GETCHUNK, fileId, chunkNo, null,address, Integer.toString(Const.TCP_BASE_PORT + restores)).getBytes(StandardCharsets.US_ASCII);

            try {
                restorePacket = new DatagramPacket(header, header.length, InetAddress.getByName(mc_host), mc_port);

                System.out.println("[Peer " + id + "] Restore file " + fileId + " chunk no." + chunkNo);
                mc.send(restorePacket);
            } catch (Exception e) {
                return Error.SEND_MULTICAST_MC;
            }
        }
        
        if(version.equals(Const.VERSION_1_1)){
            int counter = 0;
            int num = backupFile.chunks.size();
            while(counter < num) {
                try {
                    connectionSocket = restoreSocket.accept();
				} catch (SocketTimeoutException e)  {
					if(restoredFiles.get(fileId) == null)
						return "RESTORED";

					else return Error.TCP_ACCEPT_CONNECTION;
				} catch (IOException e) {
                    return Error.TCP_ACCEPT_CONNECTION;
                }

                executor.execute(new ReadTCPAnswerThread(this,connectionSocket));
                counter++;
            }

        }

        return "RESTORED";
    }


    /**
     * Restore a file
     * 
     * @param request Request for the restoration of a file
     * @return String with the information about the success or not of the function
     */
    public String restore(String request) {
        request = request.trim();
        System.out.println("[Peer " + this.id + "] Restore " + request);

        File file;

        try {
            file = new File(request);
        } catch (Exception e) {
            return Error.FILE_NOT_FOUND;
        }

        String fileId = generateId(file);
        BackupFile backupFile;

        if ((backupFile = backedupFiles.get(fileId)) == null)
            return Error.FILE_NOT_BACKED_UP;

		restoredFiles.put(fileId, new RestoredFile("peer" + id + "/restored/" + request,backupFile.chunks.size()));

        return sendRestoreMsg(backupFile,fileId);
	}
    
    /**
     * Delete a file
     * @param request
     *          Request for the deletion of a file
     * @return
     *          String with the information about the success or not of the function
     */
	public String delete(String request) {
		request = request.trim();
		System.out.println("[Peer " + this.id + "] DELETE " + request);

		File file;

        try {
            file = new File(request);

        } catch (Exception e) {
            return Error.FILE_NOT_FOUND;
        }

		String fileId = generateId(file);

        if(!version.equals(Const.VERSION_1_0)){
            updateDeletedFiles(fileId);
        }

		backedupFiles.remove(fileId);

		try {
            new File("peer" + id + "/backup/" + fileId + ".ser").delete();
        } catch (Exception e) {
        }

		try{
			byte[] header = header(Const.MSG_DELETE, fileId, null, null).getBytes(StandardCharsets.US_ASCII);
			DatagramPacket deletePacket = new DatagramPacket(header, header.length, InetAddress.getByName(mc_host), mc_port);
	
			System.out.println("[Peer " + id + "] Delete file " + fileId);
			mc.send(deletePacket);
		} catch(Exception e) {
			return Error.SEND_MULTICAST_MC;
		}
	
		return "DELETED";
	}

    private void updateDeletedFiles(String fileId) {
        BackupFile bFile = backedupFiles.get(fileId);
        if(bFile == null) return;

        Enumeration<Integer> keys = bFile.chunks.keys();
        Integer key;

        while(keys.hasMoreElements()){
            key = keys.nextElement();

            Iterator<Integer> iterator = bFile.chunks.get(key).iterator();

            while(iterator.hasNext()) {
                Integer serverId = iterator.next();
            
                ConcurrentSkipListSet<String> listFiles = deletedFiles.get(serverId);
                if(listFiles == null){
                    listFiles = new ConcurrentSkipListSet<String>();
                }
                listFiles.add(fileId);
                deletedFiles.put(serverId,listFiles);
            }
        } 
    }

    /**
     * Reclaims memory space from a peer
     * @param request
     *              Details of the reclaim
     */
	public String reclaim(String request) {
		int space_requested = Integer.parseInt(request.trim());

		if(space_requested < 0)
			return Error.INVALID_SPACE;

		disk_space = space_requested * Const.KBYTES_TO_BYTES;

		PriorityQueue<Chunk> chunks = new PriorityQueue<Chunk>(storedChunks.values());

		while(space_used.get() > disk_space && !chunks.isEmpty())
		{
			Chunk chunk = chunks.poll();
			String fileId = chunk.getFileID();
			int chunkNo = chunk.getChunkNo();

			space_used.set(space_used.get() - chunk.size);			
			storedChunks.remove(chunk.id);

			byte[] header = header(Const.MSG_REMOVED, fileId, chunkNo, null).getBytes(StandardCharsets.US_ASCII);

			try {
				DatagramPacket removedPacket = new DatagramPacket(Arrays.copyOf(header, header.length), header.length, InetAddress.getByName(mc_host), mc_port);
				System.out.println("[Peer " + id + "] Removed file " + fileId + " chunk no." + chunkNo);
				mc.send(removedPacket);
			} catch(Exception e) {}

			try {
				new File("peer" + id + "/backup/" + fileId + "/chk" + chunkNo).delete();
				new File("peer" + id + "/backup/" + fileId + "/chk" + chunkNo + ".ser").delete();
			} catch (Exception e) {
			}

		}
		
		return "RECLAIMED";
	}

    /**
     * State of the Peer
     * @return
     *         Information about the state of the Peer
     */
	public String state() {
		String res = "Backed Up Files:\n";
		
		Enumeration<String> keys = backedupFiles.keys();
		String key;

		while(keys.hasMoreElements())
		{
			key = keys.nextElement();

			res += backedupFiles.get(key) + "\n";
		}
		
		res += "Stored Chunks:\n";

		keys = storedChunks.keys();

		while(keys.hasMoreElements())
		{
			key = keys.nextElement();

			res += "\tID: " + storedChunks.get(key) + "\n";
		}

		res += "Stored Capacity: " + disk_space/1000 + "\nStorage Used: " + space_used.get()/1000 + "\n";

		return res;
	}

    /**
     * Main function of the Peer
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 9) {
            System.out.println("Usage: java Peer <protocol_version> <peer_id> <remote_object_name> <MC_IP> <MC_port> <MDB_IP> <MDB_port> <MDR_IP> <MDR_port>");
            System.exit(-1);
        }

        try {
            Peer obj = new Peer(args[0], Integer.parseInt(args[1]), args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
            PeerRMI stub = (PeerRMI) UnicastRemoteObject.exportObject(obj, 0);

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
