import java.util.concurrent.ConcurrentHashMap;
import java.io.FileOutputStream;

class RestoredFile {
	String path;
	int chunksNo;
	ConcurrentHashMap<Integer,byte[]> chunks;

	RestoredFile(String path, int chunksNo){
		this.path = path;
		this.chunksNo = chunksNo;

		chunks = new ConcurrentHashMap<>();
	}

	boolean isComplete() {
		return chunksNo == chunks.size();
	}

	void createFile() {
		try {
			FileOutputStream file = new FileOutputStream(path);

			for(int i = 0; i < chunksNo; i++) {
				file.write(chunks.get(i));
			}

			file.close();
		} catch (Exception e) {}
	}

}