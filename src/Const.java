/** 
 * Collected constants of general utility
*/
public final class Const {

    /**
     * Control Messages
     */
    public static final String MSG_STORED = "STORED";
    public static final String MSG_GETCHUNK = "GETCHUNK";
    public static final String MSG_DELETE = "DELETE";
    public static final String MSG_REMOVED = "REMOVED";

    /**
     * Data Backup Messages
     */
    public static final String MDB_PUTCHUNK = "PUTCHUNK";

    /**
     * Sizes
     */
    public static final int BUFFER_SIZE = 64000;

    /**
     * Conversions
     */
    public static final int SECONDS_TO_MILI = 1000;
    public static final int KBYTES_TO_BYTES = 1000;

    /**
     * Times
     */
    public static final int SMALL_DELAY = 401;
    public static final int MEDIUM_DELAY = 1000;

    /**
     * Protocol constants
     */
    public static final int MAX_AMOUNT_OF_TRIES = 5;
    public static final int MAX_BACKUP_THREADS = 5;
    
    /**
     * Utils strings
     */
    public static final String CRLF = "\r\n\r\n";
    public static final String SHA256 = "SHA-256";

    /**
     * Available Versions
     */
    public static final String VERSION_1_0 = "1.0";
    public static final String VERSION_1_1 = "1.1";
}