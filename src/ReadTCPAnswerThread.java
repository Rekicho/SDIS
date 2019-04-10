import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class ReadTCPAnswerThread extends Thread {
    protected Socket socket;
    protected Server server;

    public ReadTCPAnswerThread(Server server, Socket clientSocket) {
        this.socket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        InputStream input;
        BufferedInputStream inFromClient;

        try {
            input = socket.getInputStream();
            inFromClient = new BufferedInputStream(input);
        } catch (Exception e) {
            System.out.println("nao devia acontecer");
            return;
        }

        byte[] chunk = new byte[64100];
        while (true) {
            int length = -1;
            try {
                length = inFromClient.read(chunk);
                interpretMessage(chunk, length);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if(length == -1){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private void interpretMessage(byte[] buffer, int length) throws Exception {
		String[] message = new String(buffer, StandardCharsets.US_ASCII).split(Const.CRLF,2);

        System.out.println("[Peer " + server.id + " MDR] " + message[0]);

		String[] args = message[0].trim().split(" ");
		
		if (Integer.parseInt(args[2]) == server.id)
			return;

		int i = 0;
		for(; i < buffer.length && buffer[i] != 13; i++);
	
		i += 4;

		if(server.restoredFiles.get(args[3]) == null)
		{
			server.restoredChunkMessages.add(args[3] + "_" + args[4]);
			return;
		}

		if(server.restoredFiles.get(args[3]).chunks.get(Integer.parseInt(args[4])) == null) {
			server.restoredFiles.get(args[3]).chunks.put(Integer.parseInt(args[4]), Arrays.copyOfRange(buffer,i,length));
			
			if(server.restoredFiles.get(args[3]).isComplete())
			{
				server.restoredFiles.get(args[3]).createFile();
				System.out.println("[Peer " + server.id + "] File " + server.restoredFiles.get(args[3]).fileName() + " restored.");
                server.restoredFiles.remove(args[3]);
                return;
			}
        }
	}
}