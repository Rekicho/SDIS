import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class MCThread extends Thread {
    private Server server;

    MCThread(Server server){
        this.server = server;
    }

    private void interpretMessage(byte[] buffer) {
        String[] args = new String(buffer, StandardCharsets.US_ASCII).trim().split(" ");

        if(!args[0].equals("STORED"))
            return;

        server.store_responses.incrementAndGet();
    }

    public void run(){
        byte[] buffer = new byte[100];
        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                server.mc.receive(receivePacket);
                interpretMessage(buffer);
            } catch (Exception e) {
                return;
            }
        }
    }
}