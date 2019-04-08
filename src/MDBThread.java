import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Arrays;

/**
 * Thread for the Multicast Data Backup Channel
 */
public class MDBThread implements Runnable {
    
    /**
	 * Peer associated with the Thread
	 */
    private Server server;

    /**
	 * Constructor for the Multicast Data Backup Thread
	 * @param server
	 * 			Peer associated with the Thread
	 */
    MDBThread(Server server) {
        this.server = server;
    }

    /**
	 * Interprets the message received
	 * @param buffer
	 * 			Buffer with the message content to be interpreted
	 */
    private void interpretMessage(byte[] buffer, int length) throws Exception {
        String[] message = new String(buffer, StandardCharsets.US_ASCII).split("\r\n\r\n",2);

        System.out.println("[Peer " + server.id + " MDB] " + message[0]);

        String[] args = message[0].trim().split(" ");

        if (Integer.parseInt(args[2]) == server.id || server.backedupFiles.get(args[3]) != null)
            return;

        byte[] response = server.header("STORED", args[3], Integer.parseInt(args[4]), null).getBytes();
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, InetAddress.getByName(server.mc_host), server.mc_port);

		if (server.storedChunks.get(args[3] + "_" + args[4]) != null)
		{
			server.storedChunks.get(args[3] + "_" + args[4]).storedServers.set(1);
			Random r = new Random();
			Thread.sleep(r.nextInt(401));
	
			server.mc.send(responsePacket);
			return;
		}

		int i = 0;
		for(; i < buffer.length && buffer[i] != 13; i++);
	
		i += 4;
	
		int body_length = length - i - 4;

		if(server.space_used.get() + body_length > server.disk_space)
			return;

        server.storedChunks.put(args[3] + "_" + args[4], new Chunk(args[3] + "_" + args[4], body_length, Integer.parseInt(args[5])));

		server.space_used.set(server.space_used.get() + body_length);
		
		Random r = new Random();
		Thread.sleep(r.nextInt(401));

		server.mc.send(responsePacket);
		
        new File("peer" + server.id + "/backup/" + args[3]).mkdirs();
        FileOutputStream writer = new FileOutputStream("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4]);
        try{writer.write(Arrays.copyOfRange(buffer,i,length));
        writer.close();}catch(Exception e){}

        server.storedChunks.get(args[3] + "_" + args[4]).save("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4] + ".ser");
    }

    /**
	 * Listener for the thread
	 */
    public void run() {
        byte[] buffer = new byte[64100];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                server.mdb.receive(receivePacket);
            } catch (Exception e) {
                System.err.println("MDB channel error");
                System.exit(0);
            }
            server.executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        interpretMessage(buffer, receivePacket.getLength());
                    } catch (Exception e) {
						System.err.println("MDB channel error");
						e.printStackTrace();
                        System.exit(0);
                    }
                }
            });
        }
    }
}
