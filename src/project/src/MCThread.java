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

        if(server.backedupFiles.get(args[3]) != null)
            server.backedupFiles.get(args[3]).chunks.get(Integer.parseInt(args[4])).add(Integer.parseInt(args[2]));

        else if(server.storedChunks.get(args[3] + "_" + args[4]) != null)
            server.storedChunks.get(args[3] + "_" + args[4]).storedServers.incrementAndGet();
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