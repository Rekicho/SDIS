import java.util.concurrent.ConcurrentHashMap;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread created to handle the backup message sending
 */
public class BackupThread implements Runnable {

	/**
	 * Peer associated with the sender
	 */
	Peer peer;

	/**
	 * Map that associates the chunk number to the datagram packet with the information to be sent
	 */
	ConcurrentHashMap<Integer, DatagramPacket> packets;

	/**
	 * Counter of the chunk beeing sent
	 */
	AtomicInteger actualChunk;

	/**
	 * Object that contains the information about the file to be backedup
	 */
	BackupFile backupFile;

	/**
	 * Constructor of the thread
	 * @param peer
	 * 				Sender of the chunks
	 * @param packets
	 * 				Hash map with the information to be sent
	 * @param actualChunk
	 * 				Chunk that is beeing sent by the thread
	 * @param backupFile
	 * 				Class with the information about the desired replication degree of the file
	 */
	BackupThread(Peer peer, ConcurrentHashMap<Integer, DatagramPacket> packets, AtomicInteger actualChunk, BackupFile backupFile) {
		this.peer = peer;
		this.packets = packets;
		this.actualChunk = actualChunk;
		this.backupFile = backupFile;
	}

	/**
	 * Main function of the thread. Sends the chunks to the multicast data backup channel
	 */
	public void run() {
		try {
            while(true) {
                int tries = 1;
				int chunkNumber = actualChunk.getAndIncrement();

				if(chunkNumber >= packets.size())
        			return;

                do {
                    System.out.println("[Peer " + peer.id + "] Send chunk " + chunkNumber + " from " + backupFile.fileID + "(try n " + tries + ")");
                    peer.mdb.send(packets.get(chunkNumber));

                    Thread.sleep(tries * Const.SECONDS_TO_MILI);

                    if (backupFile.chunks.get(chunkNumber).size() >= backupFile.replicationDegree)
                        break;

                    tries++;
                } while (tries <= Const.MAX_AMOUNT_OF_TRIES);
            }
        } catch (Exception e) {
        }
	}

}