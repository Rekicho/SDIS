import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.Arrays;

/**
 * Thread for the Multicast Control Channel
 */
public class MCThread implements Runnable {

	/**
	 * Peer associated with the Thread
	 */
	private Server server;

	/**
	 * Constructor for the Multicast Control Channel Thread
	 * 
	 * @param server Peer associated with the Thread
	 */
	MCThread(Server server) {
		this.server = server;
	}

	/**
	 * Deletes a folder
	 * 
	 * @param folder Folder to be deleted
	 */
	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	/**
	 * Thread received a storage message. This function handles it
	 * @param serverId
	 * 				Peer from which the request came
	 * @param fileId
	 * 				File id to be stored
	 * @param chunkNo
	 * 				Number of the chunk to be stored
	 */
	private void receivedStorageMsg(int serverId, String fileId, int chunkNo) {
		String chunkFileName = fileId + "_" + chunkNo;
		BackupFile backedUpFile = server.backedupFiles.get(fileId);
		Chunk chunk = server.storedChunks.get(chunkFileName);

		if (backedUpFile != null) {
			backedUpFile.chunks.get(chunkNo).add(serverId);
			backedUpFile.save("peer" + server.id + "/backup/" + fileId + ".ser");
		} else if (chunk != null) {
			chunk.storedServers.incrementAndGet();
			chunk.save("peer" + server.id + "/backup/" + fileId + "/chk" + chunkNo + ".ser");
		}
	}

	/**
	 * Sleeps for a random amount of time from 0 to argument 'time'
	 * @param time
	 * 				Max time to be waited
	 */
	private void sleepRandom(int time) {
		try {
			Random r = new Random();
			Thread.sleep(r.nextInt(time));
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * Sends a chunk
	 * @param version
	 * 				Version of the protocol to be used
	 * @param message
	 * 				Message to be sent
	 * @param fileId
	 * 				Id of the file which the chunk is a part of
	 * @param chunkNo
	 * 				Number of the chunk that is contained in the message
	 */
	private void sendChunk(String version, byte[] message, String fileId, int chunkNo) {
		DatagramPacket chunkPacket;
		String chunkFileName = fileId + "_" + chunkNo;

		if (version.equals(Const.VERSION_1_0)) {
			try {
				chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(server.mdr_host),
						server.mdr_port);
			} catch (Exception e) {
				return;
			}

			sleepRandom(Const.SMALL_DELAY);

			if (server.restoredChunkMessages.contains(chunkFileName)) {
				server.restoredChunkMessages.remove(chunkFileName);
				return;
			}

			System.out.println("[Peer " + server.id + "] Sending restore file " + fileId + " chunk no. " + chunkNo);

			try {
				server.mdr.send(chunkPacket);
			} catch (Exception e) {
			}
		} else if (version.equals(Const.VERSION_1_1)) {
			try {
				Socket clientSocket = new Socket("localhost", 6789);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				System.out.println("Wrote: " + message.length + " bytes");
				
				outToServer.write(message,0,message.length);
				clientSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void receivedGetChunkMsg(String version, String fileId, int chunkNo) {
		String chunkFileName = fileId + "_" + chunkNo;
		if(server.storedChunks.get(chunkFileName) == null)
			return;

		int filesize;
		FileInputStream fileToRestore;
		byte[] header = (server.header("CHUNK", fileId, chunkNo, null)).getBytes();
		byte[] new_buffer = new byte[Const.BUFFER_SIZE];

		try {
			fileToRestore = new FileInputStream(new File("peer" + server.id + "/backup/" + fileId + "/chk" + chunkNo));
			filesize = fileToRestore.read(new_buffer);
			fileToRestore.close();
		} catch (Exception e) {
			return;
		}

		byte[] message = new byte[header.length + filesize];
		System.arraycopy(header, 0, message, 0, header.length);
		System.arraycopy(new_buffer, 0, message, header.length, filesize);

		sendChunk(version,message,fileId,chunkNo);
	}

	private void receivedDeleteMsg(String fileId) {
		Enumeration<String> keys = server.storedChunks.keys();
		String key;
		while(keys.hasMoreElements()) {
			key = keys.nextElement();
			if(key.contains(fileId)){
				server.space_used.set(server.space_used.get() - server.storedChunks.get(key).size);
				server.storedChunks.remove(key);
			}	
		}

		try {
			deleteFolder(new File("peer" + server.id + "/backup/" + fileId));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void receivedRemovedMsg(int serverId, String fileId, int chunkNo) {
		BackupFile backedUpFile = server.backedupFiles.get(fileId);
		Chunk chunk = server.storedChunks.get(fileId + "_" + chunkNo);
		String pathFileId = "peer" + server.id + "/backup/" + fileId;

		if(backedUpFile != null) {
			backedUpFile.chunks.get(chunkNo).remove(serverId);
			backedUpFile.save(pathFileId + ".ser");
		}
		else if(chunk != null) {			
			int actualRepDeg = chunk.storedServers.decrementAndGet();
			chunk.save(pathFileId + "/chk" + chunkNo + ".ser");
		
			if(actualRepDeg >= chunk.expectedReplicationDegree)
				return;

			sleepRandom(Const.SMALL_DELAY);

			if(actualRepDeg < chunk.storedServers.get())
				return;

			try {
				byte[] chunkBuffer = new byte[Const.BUFFER_SIZE];
				byte[] header = server.header("PUTCHUNK", fileId, chunkNo, chunk.expectedReplicationDegree).getBytes();
				int tries = 1;
				File chunkFile = new File(pathFileId + "/chk" + chunkNo);
				InputStream chunkToBackup = new FileInputStream(chunkFile);
				int count = chunkToBackup.read(chunkBuffer);
				byte[] message = new byte[header.length + count];

				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(chunkBuffer, 0, message, header.length, count);
				
				DatagramPacket chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(server.mdb_host), server.mdb_port);

				do {
					System.out.println("[Peer " + server.id + "] Send chunk " + chunkNo + " from " + fileId + "(try n " + tries + ")");
					server.mdb.send(chunkPacket);
					
					Thread.sleep(tries * Const.SECONDS_TO_MILI);

					if(chunk.storedServers.get() >= chunk.expectedReplicationDegree)
						break;

					tries++;
				}
				while(tries <= Const.MAX_AMOUNT_OF_TRIES);

				chunkToBackup.close();
			} catch (Exception e) {
				return;
			}			
		}
	}

	/**
	 * Interprets the message received
	 * @param buffer
	 * 			Buffer with the message content to be interpreted
	 */
    private void interpretMessage(byte[] buffer) {
        System.out.println("[Peer " + server.id + " MC] " + (new String(buffer).trim()));

        String[] args = new String(buffer, StandardCharsets.US_ASCII).trim().split(" ");
		String messageType = args[0];
		String version = args[1];
		int serverId = Integer.parseInt(args[2]);
		String fileId = args[3];
		int chunkNo;

        if(serverId == server.id)
			return;

		switch(messageType){
			case Const.MSG_STORED:
				chunkNo = Integer.parseInt(args[4]);
				receivedStorageMsg(serverId,fileId,chunkNo);
				break;
			case Const.MSG_GETCHUNK:
				chunkNo = Integer.parseInt(args[4]);
				receivedGetChunkMsg(version,fileId, chunkNo);
				break;
			case Const.MSG_DELETE:
				receivedDeleteMsg(fileId);
				break;
			case Const.MSG_REMOVED:
				chunkNo = Integer.parseInt(args[4]);
				receivedRemovedMsg(serverId, fileId, chunkNo);
				break;
			default:
				break;
		}	
    }

	/**
	 * Listener for the thread
	 */
    public void run(){
        byte[] buffer = new byte[100];
        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                server.mc.receive(receivePacket);
            } catch (Exception e) {
                System.err.println("MC channel error");
                System.exit(0);
			}
			byte[] newBuffer = Arrays.copyOf(buffer,buffer.length);
            server.executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        interpretMessage(newBuffer);
                    } catch (Exception e) {
                        System.err.println("MC channel error");
                        System.exit(0);
                    }
                }
            });
        }
    }
}