import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Arrays;

public class MDBThread implements Runnable {
    private Server server;

    MDBThread(Server server) {
        this.server = server;
    }

    private void interpretMessage(byte[] buffer, int length) throws Exception {
        String[] message = new String(buffer, StandardCharsets.US_ASCII).split("\r\n\r\n",2);

        System.out.println("[Peer " + server.id + " MDB] " + message[0]);

        String[] args = message[0].trim().split(" ");

        if (Integer.parseInt(args[2]) == server.id)
            return;

        byte[] response = server.header("STORED", args[3], Integer.parseInt(args[4]), null).getBytes();
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, InetAddress.getByName(server.mc_host), server.mc_port);

        Random r = new Random();
        Thread.sleep(r.nextInt(401));

        server.mc.send(responsePacket);

        if (server.storedChunks.get(args[3] + "_" + args[4]) != null)
            return;

		int i = 0;
		for(; i < buffer.length && buffer[i] != 13; i++);
	
		i += 4;
	
		int body_length = length - i - 4;

        server.storedChunks.put(args[3] + "_" + args[4], new Chunk(Integer.parseInt(args[4]), body_length, Integer.parseInt(args[5])));

        server.space_used += body_length;
        new File("peer" + server.id + "/backup/" + args[3]).mkdirs();
        FileOutputStream writer = new FileOutputStream("peer" + server.id + "/backup/" + args[3] + "/chk" + args[4]);
        try{writer.write(Arrays.copyOfRange(buffer,i,length));
        writer.close();}catch(Exception e){}

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
						e.printStackTrace();
                        System.exit(0);
                    }
                }
            });
        }
    }
}
