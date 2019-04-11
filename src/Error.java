/**
 * Error messages
 */
public final class Error {

    /**
     * File Errors
     */
    public static final String FILE_NOT_FOUND = "File Not Found";
    public static final String FILE_IO = "File I/O Error";
    public static final String FILE_NOT_BACKED_UP = "File was not backed up previosly";

    /**
     * TCP Errors
     */
    public static final String TCP_SERVER_SOCKET_CREATION = "TCP Server Socket failed to be created";
    public static final String TCP_ACCEPT_CONNECTION = "TCP Server failed to connect to client";

    /**
     * Multicast Errors
     */
    public static final String SEND_MULTICAST_MC = "Error sending datagram packet to multicast control channel";
    public static final String SEND_MULTICAST_MDR = "Error sending datagram packet to multicast data recovery channel";
    public static final String SEND_MULTICAST_MDB = "Error sending datagram packet to multicast data backup channel";


    /**
     * Edge cases
     */
    public static final String INVALID_SPACE = "Invalid space size";
}