/**
 * Error messages
 */
public final class Error {

     /**
     * Version Errors
     */
     public static final String NOT_SUPPORTED = "Enhancement not supported on current version";

     /**
     * File Errors
     */
     public static final String FILE_NOT_FOUND = "File Not Found";
     public static final String FILE_IO = "File I/O Error";
     public static final String FILE_NOT_BACKED_UP = "File was not backed up previosly";

     /**
     * TCP Errors
     */
     public static final String TCP_SERVER_SOCKET_CREATION = "TCP Peer Socket failed to be created";
     public static final String TCP_ACCEPT_CONNECTION = "TCP Peer failed to connect to client";
     public static final String FAILED_TO_READ_IP = "Failed to read local ip address";


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