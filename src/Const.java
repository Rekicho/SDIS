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
     * Enhancement Messages
     */
    public static final String ENH_DELETED = "DELETED";
    public static final String ENH_HELLO = "HELLO";

    /**
     * Client Messages
     */
	public static final String REQ_BACKUP = "BACKUP";
	public static final String REQ_BACKUP_ENH = "BACKUPENH";
	public static final String REQ_RESTORE = "RESTORE";
	public static final String REQ_RESTORE_ENH = "RESTOREENH";
	public static final String REQ_DELETE = "DELETE";
	public static final String REQ_DELETE_ENH = "DELETEENH";
    public static final String REQ_RECLAIM = "RECLAIM";
    public static final String REQ_STATE = "STATE";

    /**
     * Data Backup Messages
     */
    public static final String MDB_PUTCHUNK = "PUTCHUNK";

    /**
     * Data Reclaim Messages
     */
    public static final String MDR_CHUNK = "CHUNK";

    /**
     * Sizes
     */
    public static final int BUFFER_SIZE = 64000;
    public static final int MAX_HEADER_SIZE = 1000;

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
    
    /**
     * Utils strings
     */
    public static final String CRLF = "\r\n\r\n";
    public static final String NEW_LINE = "\r\n";
    public static final String SHA256 = "SHA-256";

    /**
     * Versions
     */
    public static final String VERSION_1_0 = "1.0";

    /**
     * TCP 
     */
    public static final int TCP_BASE_PORT = 3333;
}