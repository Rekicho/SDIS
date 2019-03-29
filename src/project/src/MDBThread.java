import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MDBThread extends Thread {
    private Server server;

    MDBThread(Server server){
        this.server = server;
    }

    private void interpretMessage(byte[] buffer, int length) throws Exception{
        String[] message = new String(buffer, StandardCharsets.US_ASCII).split("\r\n\r\n");

        String[] args = message[0].trim().split(" ");

        if(!args[0].equals("PUTCHUNK") || Integer.parseInt(args[2]) == server.id)
            return;

        byte[] response = server.header("STORED", args[3], Integer.parseInt(args[4]), null).getBytes();
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, InetAddress.getByName(server.mc_host), server.mc_port);

        Random r = new Random();
        Thread.sleep(r.nextInt(401));

        server.mc.send(responsePacket);

        if(server.storedChunks.get(args[3] + "_" + args[4]) != null)
            return;

        int body_length = length - message[0].length() - 4;

        server.storedChunks.put(args[3] + "_" + args[4], new Chunk(args[3] + " " + args[4],body_length,Integer.parseInt(args[5])));

        PrintWriter writer = new PrintWriter(args[3] + "_" + args[4], "ASCII");
        writer.print(message[1].substring(0,body_length));
        writer.close();
    }

    public void run() {
        byte[] buffer = new byte[64100];

        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                server.mdb.receive(receivePacket);
                interpretMessage(buffer, receivePacket.getLength());
            } catch (Exception e) {
                return;
            }
        }
    }
}
