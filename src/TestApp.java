import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            System.exit(-1);
        }

        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            ServerRMI stub = (ServerRMI) registry.lookup(args[0]);

            switch (args[1]) {
                case "BACKUP":
                    if(args.length != 4)
                        System.out.println("Usage: java TestApp <peer_ap> BACKUP <file> <replication_degree>");

                    String backup = args[2] + " " + args[3];
                    String response = stub.backup(backup);
                    System.out.println("BACKUP " + backup + " : " + response);
                    break;

                default: System.exit(0);
            }

        } catch (Exception e) {
            System.err.println("TestApp exception: " + e.toString());
            e.printStackTrace();
        }
    }
}