import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class MCThread implements Runnable {
    private Server server;

    MCThread(Server server){
        this.server = server;
    }

    private void interpretMessage(byte[] buffer) {
        System.out.println("[Peer " + server.id + " MC] " + (new String(buffer).trim()));

        String[] args = new String(buffer, StandardCharsets.US_ASCII).trim().split(" ");

        if(!args[0].equals("STORED") || Integer.parseInt(args[2]) == server.id)
			return;

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
    }

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
            server.executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        interpretMessage(buffer);
                    } catch (Exception e) {
                        System.err.println("MC channel error");
                        System.exit(0);
                    }
                }
            });
        }
    }
}