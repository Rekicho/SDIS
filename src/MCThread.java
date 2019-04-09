import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
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
	 * @param server
	 * 			Peer associated with the Thread
	 */
    MCThread(Server server){
        this.server = server;
	}
	
	/**
	 * Deletes a folder
	 * @param folder
	 * 			Folder to be deleted
	 */
	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) {
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	/**
	 * Interprets the message received
	 * @param buffer
	 * 			Buffer with the message content to be interpreted
	 */
    private void interpretMessage(byte[] buffer) {
        System.out.println("[Peer " + server.id + " MC] " + (new String(buffer).trim()));

        String[] args = new String(buffer, StandardCharsets.US_ASCII).trim().split(" ");

        if(Integer.parseInt(args[2]) == server.id)
			return;

        if(args[0].equals("STORED")) {
			if(server.backedupFiles.get(args[3]) != null)
			{
				server.backedupFiles.get(args[3]).chunks.get(Integer.parseInt(args[4])).add(Integer.parseInt(args[2]));
				server.backedupFiles.get(args[3]).save("peer" + server.id + "/backup/" + args[3] + ".ser");
			}
	
			else if(server.storedChunks.get(args[3] + "_" + args[4]) != null)
			{
				server.storedChunks.get(args[3] + "_" + args[4]).storedServers.incrementAndGet();
				server.storedChunks.get(args[3] + "_" + args[4]).save("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4] + ".ser");
			}
		} else if (args[0].equals("GETCHUNK")) {
			if(server.storedChunks.get(args[3] + "_" + args[4]) == null)
				return;

			FileInputStream fileToRestore;

			try {
				fileToRestore = new FileInputStream(new File("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4]));
			} catch (Exception e) {
				return;
			}

			byte[] header = (server.header("CHUNK", args[3], Integer.parseInt(args[4]), null)).getBytes();
			byte[] new_buffer = new byte[64000];
			int filesize;
			
			try {
				filesize = fileToRestore.read(new_buffer);
				fileToRestore.close();
			} catch (Exception e) {
				return;
			}

			byte[] message = new byte[header.length + filesize];

            System.arraycopy(header, 0, message, 0, header.length);
			System.arraycopy(new_buffer, 0, message, header.length, filesize);

			DatagramPacket chunkPacket;
			
			try {
				chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(server.mdr_host), server.mdr_port);
			} catch (Exception e) {
				return;
			}

			try {
				Random r = new Random();
				Thread.sleep(r.nextInt(401));
			} catch (Exception e) {
				return;
			}

			if (server.restoredChunk.get(args[3] + "_" + args[4]) != null)
			{
				server.restoredChunk.remove(args[3] + "_" + args[4]);
				return;
			}
			
			System.out.println("[Peer " + server.id + "] Sending restore file " + args[3] + " chunk no. " + args[4]);
			
			try {
				server.mdr.send(chunkPacket);
			} catch (Exception e) {
			}
		} else if (args[0].equals("DELETE")) {
			Enumeration<String> keys = server.storedChunks.keys();
			String key;
			while(keys.hasMoreElements())
			{
				key = keys.nextElement();
				if(key.contains(args[3])){
					server.space_used.set(server.space_used.get() - server.storedChunks.get(key).size);
					server.storedChunks.remove(key);
				}
					
			}

			try {
				deleteFolder(new File("peer" + server.id + "/backup/" + args[3]));
			} catch (Exception e) {
			}
		} else if (args[0].equals("REMOVED")) {
			if(server.backedupFiles.get(args[3]) != null)
			{
				server.backedupFiles.get(args[3]).chunks.get(Integer.parseInt(args[4])).remove(Integer.parseInt(args[2]));
				server.backedupFiles.get(args[3]).save("peer" + server.id + "/backup/" + args[3] + ".ser");
			}
	
			else if(server.storedChunks.get(args[3] + "_" + args[4]) != null)
			{
				Chunk chunk = server.storedChunks.get(args[3] + "_" + args[4]);				
				int actualRepDeg = chunk.storedServers.decrementAndGet();
				chunk.save("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4] + ".ser");
			
				if(actualRepDeg >= chunk.expectedReplicationDegree)
					return;

				try {
					Random r = new Random();
					Thread.sleep(r.nextInt(401));
				} catch (Exception e) {
					return;
				}

				if(chunk.storedServers.get() > actualRepDeg)
					return;

				try {
					byte[] chunkBuffer = new byte[64000];
					byte[] header = server.header("PUTCHUNK", args[3], Integer.parseInt(args[4]), chunk.expectedReplicationDegree).getBytes();
					
					int tries = 1;
					File chunkFile = new File("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4]);
					InputStream chunkToBackup = new FileInputStream(chunkFile);
					int count = chunkToBackup.read(chunkBuffer);

					byte[] message = new byte[header.length + count];

                	System.arraycopy(header, 0, message, 0, header.length);
					System.arraycopy(chunkBuffer, 0, message, header.length, count);
					
					DatagramPacket chunkPacket = new DatagramPacket(message, message.length, InetAddress.getByName(server.mdb_host), server.mdb_port);

					do {
						System.out.println("[Peer " + server.id + "] Send chunk " + args[4] + " from " + args[3] + "(try n " + tries + ")");
						server.mdb.send(chunkPacket);
						
						Thread.sleep(tries * 1000);
	
						if(chunk.storedServers.get() >= chunk.expectedReplicationDegree)
							break;
	
						tries++;
					}
					while(tries <= 5);


				} catch (Exception e) {return;}			
			}
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