import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Thread for the Multicast Data Recovery Channel
 */
public class MDRThread implements Runnable {
    
    /**
	 * Peer associated with the Thread
	 */
    private Peer peer;

    /**
	 * Constructor for the Multicast Data Recovery Thread
	 * @param peer
	 * 			Peer associated with the Thread
	 */
    MDRThread(Peer peer){
        this.peer = peer;
	}

    /**
	 * Interprets the message received
	 * @param buffer
	 * 			Buffer with the message content to be interpreted
	 */
	private void interpretMessage(byte[] buffer, int length) throws Exception {
		String[] message = new String(buffer, StandardCharsets.US_ASCII).split(Const.CRLF,2);

        System.out.println("[Peer " + peer.id + " MDR] " + message[0]);

		String[] args = message[0].trim().split(" ");
		
		if (!args[0].equals(Const.MDR_CHUNK) || Integer.parseInt(args[2]) == peer.id)
			return;

		int i = 0;
		for(; i < buffer.length && buffer[i] != 13; i++);
	
		i += Const.CRLF.length();

		RestoredFile restored = peer.restoredFiles.get(args[3]);

		if(restored == null)
		{
			peer.restoredChunkMessages.add(args[3] + "_" + args[4]);
			return;
		}

		if(restored.chunks.get(Integer.parseInt(args[4])) == null) {
			restored.chunks.put(Integer.parseInt(args[4]), Arrays.copyOfRange(buffer,i,length));
			
			if(restored.isComplete())
			{
				restored.createFile();
				System.out.println("[Peer " + peer.id + "] File " + restored.fileName() + " restored.");
				peer.restoredFiles.remove(args[3]);
			}
		}
	}

    /**
	 * Listener for the thread
	 */
    public void run() {
        byte[] buffer = new byte[Const.BUFFER_SIZE + Const.MAX_HEADER_SIZE];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                peer.mdr.receive(receivePacket);
            } catch (Exception e) {
                System.err.println(Error.SEND_MULTICAST_MDR);
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
                        System.err.println(Error.SEND_MULTICAST_MDR);
                        System.exit(0);
                    }
                }
            });
        }
    }
}
