import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Plate extends Remote {
    String request(String request) throws RemoteException;
}
