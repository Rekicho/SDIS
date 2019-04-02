import java.io.File;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MDBThread implements Runnable {
    private Server server;

    MDBThread(Server server) {
        this.server = server;
    }

    private void interpretMessage(byte[] buffer, int length) throws Exception {
        String[] message = new String(buffer, StandardCharsets.US_ASCII).split("\r\n\r\n",2);

        String[] args = message[0].trim().split(" ");

        if (!args[0].equals("PUTCHUNK") || Integer.parseInt(args[2]) == server.id)
            return;

        byte[] response = server.header("STORED", args[3], Integer.parseInt(args[4]), null).getBytes();
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, InetAddress.getByName(server.mc_host), server.mc_port);

        Random r = new Random();
        Thread.sleep(r.nextInt(401));

        server.mc.send(responsePacket);

        if (server.storedChunks.get(args[3] + "_" + args[4]) != null)
            return;

        int body_length = length - message[0].length() - 4;

        server.storedChunks.put(args[3] + "_" + args[4], new Chunk(args[3] + " " + args[4], body_length, Integer.parseInt(args[5])));

        new File("peer" + server.id + "/backup/" + args[3]).mkdirs();
        PrintWriter writer = new PrintWriter("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4], "ASCII");
        writer.print(message[1].substring(0, body_length));
        writer.close();

        server.storedChunks.get(args[3] + "_" + args[4]).save("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4] + ".ser");
    }

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
                        System.exit(0);
                    }
                }
            });
        }
    }
}
