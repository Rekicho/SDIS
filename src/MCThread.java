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
	private Peer peer;

	/**
	 * Constructor for the Multicast Control Channel Thread
	 * 
	 * @param peer Peer associated with the Thread
	 */
	MCThread(Peer peer) {
		this.peer = peer;
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
	 * @param peerId
	 * 				Peer from which the request came
	 * @param fileId
	 * 				File id to be stored
	 * @param chunkNo
	 * 				Number of the chunk to be stored
	 */
	private void receivedStorageMsg(int peerId, String fileId, int chunkNo) {
		String chunkFileName = fileId + "_" + chunkNo;
		BackupFile backedUpFile = peer.backedupFiles.get(fileId);
		Chunk chunk = peer.storedChunks.get(chunkFileName);

		if (backedUpFile != null) {
			backedUpFile.chunks.get(chunkNo).add(peerId);
			backedUpFile.save("peer" + peer.id + "/backup/" + fileId + ".ser");
		} else if (chunk != null) {
			chunk.storedPeers.incrementAndGet();
			chunk.save("peer" + peer.id + "/backup/" + fileId + "/chk" + chunkNo + ".ser");
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
	 * 				Number of the chunk that is contained in the
	 */
	private void sendChunk(String version, byte[] message, String fileId, int chunkNo, String address, String port) {
		DatagramPacket chunkPacket;
		String chunkFileName = fileId + "_" + chunkNo;

		try {
			chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(peer.mdr_host),
					peer.mdr_port);
		} catch (Exception e) {
			return;
		}

		if (version.equals(Const.VERSION_1_0) || peer.version.equals(Const.VERSION_1_0)) {
			sleepRandom(Const.SMALL_DELAY);

			if (peer.restoredChunkMessages.contains(chunkFileName)) {
				peer.restoredChunkMessages.remove(chunkFileName);
				return;
			}

			System.out.println("[Peer " + peer.id + "] Sending restore file " + fileId + " chunk no. " + chunkNo);

			try {
				peer.mdr.send(chunkPacket);
			} catch (Exception e) {
			}
		} else {
			sleepRandom(Const.SMALL_DELAY / 2);

			if (peer.restoredChunkMessages.contains(chunkFileName)) {
				peer.restoredChunkMessages.remove(chunkFileName);
				return;
			}

			System.out.println("[Peer " + peer.id + "] Sending restore file " + fileId + " chunk no. " + chunkNo);

			try {
				peer.mdr.send(chunkPacket);
			} catch (Exception e) {
			}
			
			try {
				Socket clientSocket = new Socket(address, Integer.parseInt(port));
				DataOutputStream outToPeer = new DataOutputStream(clientSocket.getOutputStream());
				
				outToPeer.write(message,0,message.length);
				clientSocket.close();
			} catch (Exception e) {
				System.err.println("Failed to open socket");
			}
		}
	}

	/**
	 * Handles the message of a get chunk
	 * @param version
	 * 				Version of the protocol to be used
	 * @param fileId
	 * 				Id of the file
	 * @param chunkNo
	 * 				Number of the chunk
	 */
	private void receivedGetChunkMsg(String version, String fileId, int chunkNo, String address, String port) {
		String chunkFileName = fileId + "_" + chunkNo;
		if(peer.storedChunks.get(chunkFileName) == null)
			return;

		int filesize;
		FileInputStream fileToRestore;
		byte[] header = (peer.header("CHUNK", fileId, chunkNo, null)).getBytes(StandardCharsets.US_ASCII);
		byte[] new_buffer = new byte[Const.BUFFER_SIZE];

		try {
			fileToRestore = new FileInputStream(new File("peer" + peer.id + "/backup/" + fileId + "/chk" + chunkNo));
			filesize = fileToRestore.read(new_buffer);
			fileToRestore.close();
		} catch (Exception e) {
			return;
		}

		byte[] message;

		if (version.equals(Const.VERSION_1_0) || peer.version.equals(Const.VERSION_1_0)) { 
			message = new byte[header.length + filesize];
			System.arraycopy(header, 0, message, 0, header.length);
			System.arraycopy(new_buffer, 0, message, header.length, filesize);
		} else {
			message = header;
		}
		
		sendChunk(version,message,fileId,chunkNo,address, port);
	}

	/**
	 * Handles the message of a delete message
	 * @param fileId
	 * 				Id of the file
	 */
	private void receivedDeleteMsg(String fileId) {
		Enumeration<String> keys = peer.storedChunks.keys();
		String key;
		while(keys.hasMoreElements()) {
			key = keys.nextElement();
			if(key.contains(fileId)){
				peer.space_used.set(peer.space_used.get() - peer.storedChunks.get(key).size);
				peer.storedChunks.remove(key);
			}	
		}

		try {
			deleteFolder(new File("peer" + peer.id + "/backup/" + fileId));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles the removed message
	 * @param peerId
	 * 				Identifier of the peer
	 * @param fileId
	 * 				Identifier of the file
	 * @param chunkNo
	 * 				Number of the chunk
	 */
	private void receivedRemovedMsg(int peerId, String fileId, int chunkNo) {
		BackupFile backedUpFile = peer.backedupFiles.get(fileId);
		Chunk chunk = peer.storedChunks.get(fileId + "_" + chunkNo);
		String pathFileId = "peer" + peer.id + "/backup/" + fileId;

		if(backedUpFile != null) {
			backedUpFile.chunks.get(chunkNo).remove(peerId);
			backedUpFile.save(pathFileId + ".ser");
		}
		else if(chunk != null) {			
			int actualRepDeg = chunk.storedPeers.decrementAndGet();
			chunk.save(pathFileId + "/chk" + chunkNo + ".ser");
		
			if(actualRepDeg >= chunk.expectedReplicationDegree)
				return;

			sleepRandom(Const.SMALL_DELAY);

			if(actualRepDeg < chunk.storedPeers.get())
				return;

			try {
				byte[] chunkBuffer = new byte[Const.BUFFER_SIZE];
				byte[] header = peer.header(Const.MDB_PUTCHUNK, fileId, chunkNo, chunk.expectedReplicationDegree).getBytes(StandardCharsets.US_ASCII);
				int tries = 1;
				File chunkFile = new File(pathFileId + "/chk" + chunkNo);
				InputStream chunkToBackup = new FileInputStream(chunkFile);
				int count = chunkToBackup.read(chunkBuffer);
				byte[] message = new byte[header.length + count];

				System.arraycopy(header, 0, message, 0, header.length);
				System.arraycopy(chunkBuffer, 0, message, header.length, count);
				
				DatagramPacket chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(peer.mdb_host), peer.mdb_port);

				do {
					System.out.println("[Peer " + peer.id + "] Send chunk " + chunkNo + " from " + fileId + "(try n " + tries + ")");
					peer.mdb.send(chunkPacket);
					
					Thread.sleep(tries * Const.SECONDS_TO_MILI);

					if(chunk.storedPeers.get() >= chunk.expectedReplicationDegree)
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
        System.out.println("[Peer " + peer.id + " MC] " + (new String(buffer).trim()));

        String[] args = new String(buffer, StandardCharsets.US_ASCII).trim().split(" ");
		String messageType = args[0];
		String version = args[1];
		int peerId = Integer.parseInt(args[2]);
		String fileId = args[3];
		int chunkNo;
		String address = null;
		String port = null;

        if(peerId == peer.id)
			return;

		switch(messageType){
			case Const.MSG_STORED:
				chunkNo = Integer.parseInt(args[4]);
				receivedStorageMsg(peerId,fileId,chunkNo);
				break;
			case Const.MSG_GETCHUNK:
				chunkNo = Integer.parseInt(args[4]);
				if(!version.equals(Const.VERSION_1_0)){
					address = args[6].substring(2);
					port = args[7];
				}
				
				receivedGetChunkMsg(version,fileId, chunkNo,address, port);
				break;
			case Const.MSG_DELETE:
				receivedDeleteMsg(fileId);
				break;
			case Const.MSG_REMOVED:
				chunkNo = Integer.parseInt(args[4]);
				receivedRemovedMsg(peerId, fileId, chunkNo);
				break;
			default:
				break;
		}	
    }

	/**
	 * Listener for the thread
	 */
    public void run(){
        byte[] buffer = new byte[1000];
        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                peer.mc.receive(receivePacket);
            } catch (Exception e) {
                System.err.println(Error.SEND_MULTICAST_MC);
                System.exit(0);
			}
			byte[] newBuffer = Arrays.copyOf(buffer,buffer.length);
            peer.executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        interpretMessage(newBuffer);
                    } catch (Exception e) {
                        System.err.println(Error.SEND_MULTICAST_MC);
                        System.exit(0);
                    }
                }
            });
        }
    }
}