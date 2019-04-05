import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MDRThread implements Runnable {
    private Server server;

    MDRThread(Server server){
        this.server = server;
	}

	private void interpretMessage(byte[] buffer, int length) throws Exception {
		String[] message = new String(buffer, StandardCharsets.US_ASCII).split("\r\n\r\n",2);

        System.out.println("[Peer " + server.id + " MDR] " + message[0]);

		String[] args = message[0].trim().split(" ");
		
		if (Integer.parseInt(args[2]) == server.id)
			return;

		int i = 0;
		for(; i < buffer.length && buffer[i] != 13; i++);
	
		i += 4;
	
		int body_length = length - i - 4;

		server.restoredChunk.put(args[3] + "_" + args[4], Arrays.copyOfRange(buffer,i,length));
	}

    public void run() {
        byte[] buffer = new byte[64100];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                server.mdr.receive(receivePacket);
            } catch (Exception e) {
                System.err.println("MDR channel error");
                System.exit(0);
            }
            server.executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        interpretMessage(buffer, receivePacket.getLength());
                    } catch (Exception e) {
                        System.err.println("MDR channel error");
                        System.exit(0);
                    }
                }
            });
        }
    }
}
