import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class Server implements Plate {
    private static final String REGISTER = "REGISTER";
    private static final String LOOKUP = "LOOKUP";

    private static final String PLATE_REGEX = "[0-9A-Z]{2}-[0-9A-Z]{2}-[0-9A-Z]{2}";

    private static final int TTL = 1;

    private static Map<String, String> plates = new HashMap<>();

    public Server() {
    }

    public String request(String request) {
        return parseRequest(request);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java lab2.ola.Server <remote_object_name>");
            System.exit(-1);
        }

        try {
            lab2.ola.Server obj = new lab2.ola.Server();
            Plate stub = (Plate) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[0], stub);

            System.out.println("lab2.ola.Server ready");
        } catch (Exception e) {
            System.err.println("lab2.ola.Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static String parseRequest(String request) {
        String[] args = request.split(" ", 3);
        if (!isValidPlate(args[1])) {
            return "INVALID PLATE FORMAT";
        }

        if (args[0].equals(REGISTER)) {
            System.out.println("register " + args[1] + " " + args[2]);
            if (args.length != 3) {
                return "INVALID N OF ARGS, EXPECTED: REGISTER <plate number> <owner name>";
            }

            return registerPlate(args[1], args[2]);
        } else if (args[0].equals(LOOKUP)) {
            System.out.println("lookup " + args[1]);
            if (args.length != 2) {
                return "INVALID N OF ARGS, EXPECTED: LOOKUP <plate number>";
            }

            return lookupPlate(args[1]);
        } else {
            return "INVALID REQUEST\nREGISTER <plate number> <owner name>\nLOOKUP <plate number>";
        }
    }

    private static String registerPlate(String plateNumber, String ownerName) {
        boolean hasPlate = plates.containsKey(plateNumber);

        if (hasPlate) {
            return "-1";
        } else {
            plates.put(plateNumber, ownerName);
            return String.valueOf(plates.size());
        }
    }

    private static String lookupPlate(String plateNumber) {
        boolean hasPlate = plates.containsKey(plateNumber);

        if (!hasPlate) {
            return "NOT_FOUND";
        } else {
            String name = plates.get(plateNumber);
            return plateNumber + " " + name;
        }
    }

    private static boolean isValidPlate(String plate) {
        return plate.matches(PLATE_REGEX);
    }
}
