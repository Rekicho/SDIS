import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Class responsible to handle requests made by TCP
 */
class ReadTCPAnswerThread implements Runnable {

    /**
     * Socket where the message is expected
     */
    protected Socket socket;

    /**
     * Server where the socket is
     */
    protected Server server;

    /**
     * Constructor for the thread responsible to handle TCP messages
     * @param server
     *              Server that received the request
     * @param clientSocket
     *              Socket where the request is passed to
     */
    public ReadTCPAnswerThread(Server server, Socket clientSocket) {
        this.socket = clientSocket;
        this.server = server;
    }

    /**
     * Message to run the thread
     */
    @Override
    public void run() {
        InputStream input;
        BufferedInputStream inFromClient;

        try {
            input = socket.getInputStream();
            inFromClient = new BufferedInputStream(input);
        } catch (Exception e) {
            return;
        }

        byte[] chunk = new byte[65000];
        int chunk_length = 0;

        while (true) {
            byte[] buffer = new byte[65000];
            int length = -1;

            try {
                length = inFromClient.read(buffer);
                
                if(length != -1)
                {
                    byte[] new_chunk = new byte[65000];
                    System.arraycopy(chunk, 0, new_chunk, 0, chunk_length);
                    System.arraycopy(buffer, 0, new_chunk, chunk_length, length);

                    chunk = new_chunk;
                    chunk_length += length;
                    continue;
                }

                interpretMessage(Arrays.copyOf(chunk, chunk_length), chunk_length);   
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
    }

    /**
     * Interprets the message from the tcp connection
     * @param buffer
     * @param length
     * @throws Exception
     */
    private void interpretMessage(byte[] buffer, int length) throws Exception {
		String[] message = new String(buffer, StandardCharsets.US_ASCII).split(Const.CRLF,2);

        System.out.println("[Peer " + server.id + " TCP] " + message[0]);

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