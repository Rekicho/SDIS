import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    private static final String REGISTER = "register";
    private static final String LOOKUP = "lookup";

    private static final String ALREADY_REGISTERED = "-1";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String ERROR = "ERROR";

    public static void main(String[] args) throws Exception {
        if (!((args.length == 4 && args[2].equals(LOOKUP)) || (args.length == 5 && args[2].equals(REGISTER)))) {
            System.out.println("Usage: java lab2.ola.Client <host_name> <remote_object_name> <oper> <opnd>*");
            System.out.println("       java lab2.ola.Client <host_name> <remote_object_name> register <plate number> <owner name>");
            System.out.println("       java lab2.ola.Client <host_name> <remote_object_name> lookup <plate number>");
            System.exit(-1);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);
            Plate stub = (Plate) registry.lookup(args[1]);

            String request = args[2].equals(LOOKUP) ?
                    args[2].toUpperCase() + " " + args[3] :
                    args[2].toUpperCase() + " " + args[3] + " " + args[4];

            String response = stub.request(request);
            System.out.println(request + ": " + (response.equals(ALREADY_REGISTERED) || response.equals(NOT_FOUND) ? ERROR : response));
        } catch (Exception e) {
            System.err.println("lab2.ola.Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
