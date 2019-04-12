import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread for the Multicast Data Backup Channel
 */
public class MDBThread implements Runnable {
    
    /**
	 * Peer associated with the Thread
	 */
    private Peer peer;

    /**
	 * Constructor for the Multicast Data Backup Thread
	 * @param peer
	 * 			Peer associated with the Thread
	 */
    MDBThread(Peer peer) {
        this.peer = peer;
    }

    /**
	 * Interprets the message received
	 * @param buffer
	 * 			Buffer with the message content to be interpreted
	 */
    private void interpretMessage(byte[] buffer, int length) throws Exception {
        String[] message = new String(buffer, StandardCharsets.US_ASCII).split(Const.CRLF,2);

        System.out.println("[Peer " + peer.id + " MDB] " + message[0]);

        String[] args = message[0].trim().split(" ");

        if (Integer.parseInt(args[2]) == peer.id || peer.backedupFiles.get(args[3]) != null)
            return;

        byte[] response = peer.header(Const.MSG_STORED, args[3], Integer.parseInt(args[4]), null).getBytes(StandardCharsets.US_ASCII);
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, InetAddress.getByName(peer.mc_host), peer.mc_port);

		Random r = new Random();
		
		if (peer.storedChunks.get(args[3] + "_" + args[4]) != null)
		{
			peer.storedChunks.get(args[3] + "_" + args[4]).storedPeers.set(1);
			Thread.sleep(r.nextInt(401));
	
			peer.mc.send(responsePacket);
			return;
		}

		int i = 0;
		for(; i < buffer.length && buffer[i] != 13; i++);
	
		i += 4;
	
		int body_length = length - i - 4;
		int free_space = peer.disk_space - peer.space_used.get(); 

		if(body_length > free_space)
		{
			System.out.println("[Peer " + peer.id + " MDB] No memory to store chunk " + args[3] + "_" + args[4] + ".");
			return;
		}

		if (!peer.version.equals(Const.VERSION_1_0))
		{
			int tries;
			if(peer.chunkTries.get(args[3] + "_" + args[4]) == null)
			{
				peer.chunkTries.put(args[3] + "_" + args[4], new AtomicInteger(1));
				tries = 1;
			}

			else tries = peer.chunkTries.get(args[3] + "_" + args[4]).incrementAndGet();

			float storeProbablity;
			
			if(tries == Const.MAX_AMOUNT_OF_TRIES)
				storeProbablity = 1;

			else storeProbablity = (0.1f * tries) + (0.5f * (((float)free_space - body_length) / peer.disk_space));
			
			System.out.println("[Peer " + peer.id + " MDB] Probability store chunk " + args[3] + "_" + args[4] + ": " + (storeProbablity * 100) + "%.");
			
			if(r.nextFloat() > storeProbablity)
			{
				System.out.println("[Peer " + peer.id + " MDB] Decided not to store chunk " + args[3] + "_" + args[4] + ".");
				return;
			}
		}

        peer.storedChunks.put(args[3] + "_" + args[4], new Chunk(args[3] + "_" + args[4], body_length, Integer.parseInt(args[5])));
		new File("peer" + peer.id + "/backup/" + args[3]).mkdirs();
		peer.storedChunks.get(args[3] + "_" + args[4]).save("peer" + peer.id + "/backup/" + args[3] + "/chk" + args[4] + ".ser");

		peer.space_used.set(peer.space_used.get() + body_length);
		
		Thread.sleep(r.nextInt(401));

		peer.mc.send(responsePacket);
		
        FileOutputStream writer = new FileOutputStream("peer" + peer.id + "/backup/" + args[3] + "/chk" + args[4]);
        try{writer.write(Arrays.copyOfRange(buffer,i,length));
        writer.close();}catch(Exception e){}
    }

    /**
	 * Listener for the thread
	 */
    public void run() {
        byte[] buffer = new byte[65000];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                peer.mdb.receive(receivePacket);
            } catch (Exception e) {
                System.err.println(Error.SEND_MULTICAST_MDB);
                System.exit(0);
			}
			byte[] newBuffer = Arrays.copyOf(buffer,buffer.length);
			int length = receivePacket.getLength();
            peer.executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        interpretMessage(newBuffer, length);
                    } catch (Exception e) {
						System.err.println(Error.SEND_MULTICAST_MDB);
						e.printStackTrace();
                        System.exit(0);
                    }
                }
            });
        }
    }
}
