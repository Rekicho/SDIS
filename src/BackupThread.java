import java.util.concurrent.ConcurrentHashMap;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupThread implements Runnable {
	Peer peer;
	ConcurrentHashMap<Integer, DatagramPacket> packets;
	AtomicInteger actualChunk;
	BackupFile backupFile;

	
	BackupThread(Peer peer, ConcurrentHashMap<Integer, DatagramPacket> packets, AtomicInteger actualChunk, BackupFile backupFile) {
		this.peer = peer;
		this.packets = packets;
		this.actualChunk = actualChunk;
		this.backupFile = backupFile;
	}

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