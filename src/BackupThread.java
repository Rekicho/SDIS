import java.util.concurrent.ConcurrentHashMap;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupThread implements Runnable {
	Peer peer;
	ConcurrentHashMap<Integer, DatagramPacket> packets;
	AtomicInteger actualChunk;
	BackupFile backupFile;
	AtomicInteger threadsleft;
	AtomicInteger errors;

	
	BackupThread(Peer peer, ConcurrentHashMap<Integer, DatagramPacket> packets, AtomicInteger actualChunk, BackupFile backupFile, AtomicInteger threadleft, AtomicInteger errors) {
		this.peer = peer;
		this.packets = packets;
		this.actualChunk = actualChunk;
		this.backupFile = backupFile;
		this.threadsleft = threadsleft;
		this.errors = errors;
	}

	public void run() {
		try {
            while(true) {
                int tries = 1;
				int chunkNumber = actualChunk.getAndIncrement();

				if(chunkNumber >= packets.size()) {
					threadsleft.decrementAndGet();
					return;
				}

                do {
                    System.out.println("[Peer " + peer.id + "] Send chunk " + chunkNumber + " from " + backupFile.fileID + "(try n " + tries + ")");
                    peer.mdb.send(packets.get(chunkNumber));

                    Thread.sleep(tries * Const.SECONDS_TO_MILI);

                    if (backupFile.chunks.get(chunkNumber).size() >= backupFile.replicationDegree) {
						threadsleft.decrementAndGet();
						return;
					}

                    tries++;
                } while (tries <= Const.MAX_AMOUNT_OF_TRIES);
            }
        } catch (Exception e) {
		}
		
		threadsleft.decrementAndGet();
		errors.incrementAndGet();
	}

}