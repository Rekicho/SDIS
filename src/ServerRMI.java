import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for the RMIRegistry functions
 */
public interface ServerRMI extends Remote {

	/**
	 * Allows the backup of a document amongst the peers of the network
	 * @param request
	 * 				String containing the details about the request
	 * @return
	 * 				String containing the status of the execution
	 * @throws RemoteException
	 */
	String backup(String request) throws RemoteException;
	
	/**
	 * Allows the deletion of a document saved in the peers of the network
	 * @param request
	 * 				String containing the details about the request
	 * @return
	 * 				String containing the status of the execution
	 * @throws RemoteException
	 */
	String delete(String request) throws RemoteException;

	/**
	 * Allows the restoration of a document saved in the other peers of the network, the restored file will be saved in the peer-iniatior
	 * @param request
	 * 				String containing the details about the request
	 * @return
	 * 				String containing the status of the execution
	 * @throws RemoteException
	 */
	String restore(String request) throws RemoteException;

	/**
	 * Allows the reclaim of space of the initiator-peer
	 * @param request
	 * 				String containing the details about the request
	 * @return
	 * 				String containing the status of the execution
	 * @throws RemoteException
	 */
	String reclaim(String request) throws RemoteException;

	/**
	 * Allows the user to get the state of the initiator-peer
	 * @param request
	 * 				String containing the details about the request
	 * @return
	 * 				String containing the status of the execution
	 * @throws RemoteException
	 */
	String state() throws RemoteException;
}