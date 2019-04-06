import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for the RMIRegistry functions
 */
public interface ServerRMI extends Remote {
	String backup(String request) throws RemoteException;
	String delete(String request) throws RemoteException;
	String restore(String request) throws RemoteException;
	String state() throws RemoteException;
}