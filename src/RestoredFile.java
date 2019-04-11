import java.util.concurrent.ConcurrentHashMap;
import java.io.FileOutputStream;

/**
 * Class to save information about a restored file
 */
class RestoredFile {
	
	/**
	 * Path of the restored file
	 */
	String path;

	/**
	 * Number of chunks in a file
	 */
	int chunksNo;

	/**
	 * Map to match a chunk number to their information
	 */
	ConcurrentHashMap<Integer,byte[]> chunks;

	/**
	 * Creates a RestoredFile from path and number of chunks in file
	 * @param path
	 * 			Path of the file
	 * @param chunksNo
	 * 			Number of the chunks of the file
	 */
	RestoredFile(String path, int chunksNo){
		this.path = path;
		this.chunksNo = chunksNo;

		chunks = new ConcurrentHashMap<>();
	}

	/**
	 * Returns true if it has the necessary chunks to create the file
	 * @return
	 * 			If it has enough information to recreate the file
	 */
	boolean isComplete() {
		return chunksNo == chunks.size();
	}

	/**
	 * Create a file from the map of chunks
	 */
	void createFile() {
		try {
			FileOutputStream file = new FileOutputStream(path);

			for(int i = 0; i < chunksNo; i++) {
				file.write(chunks.get(i));
			}

			file.close();
		} catch (Exception e) {}
	}

	/**
	 * Get the name of the file from its path
	 * @return
	 * 			Name of the file
	 */
	String fileName() {
		String[] names = path.split("/");

		return names[names.length - 1];
	}

}