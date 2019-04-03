import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.io.File;

public class MCThread implements Runnable {
    private Server server;

    MCThread(Server server){
        this.server = server;
	}
	
	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) {
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

    private void interpretMessage(byte[] buffer) {
        System.out.println("[Peer " + server.id + " MC] " + (new String(buffer).trim()));

        String[] args = new String(buffer, StandardCharsets.US_ASCII).trim().split(" ");

        if(Integer.parseInt(args[2]) == server.id)
			return;

        if(args[0].equals("STORED")) {
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
		} else {
			Enumeration<String> keys = server.storedChunks.keys();
			String key;
			while(keys.hasMoreElements())
			{
				key = keys.nextElement();
				if(key.contains(args[3]))
					server.storedChunks.remove(args[3]);
			}

			try {
				deleteFolder(new File("peer" + server.id + "/backup/" + args[3]));
			} catch (Exception e) {
			}
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