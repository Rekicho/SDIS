import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Class to represent Client acessing a peer server
 */
public class TestApp {

	private final static String ADDRESS = "localhost";
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            System.exit(-1);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(ADDRESS);
			ServerRMI stub = (ServerRMI) registry.lookup(args[0]);
			String response;

            switch (args[1]) {
                case "BACKUP":
					if(args.length != 4)
					{
						System.out.println("Usage: java TestApp <peer_ap> BACKUP <file> <replication_degree>");
						System.exit(1);
					}

                    String backup = args[2] + " " + args[3];
                    response = stub.backup(backup);
                    System.out.println("BACKUP " + backup + " : " + response);
					break;

				case "RESTORE":
					if(args.length != 3)
					{
						System.out.println("Usage: java TestApp <peer_ap> RESTORE <file>");
						System.exit(1);
					}

                    response = stub.restore(args[2]);
                    System.out.println("RESTORE " + args[2] + " : " + response);
                    break;
				
				case "DELETE":
					if(args.length != 3)
					{
						System.out.println("Usage: java TestApp <peer_ap> DELETE <file>");
						System.exit(1);
					}

                    response = stub.delete(args[2]);
                    System.out.println("DELETE " + args[2] + " : " + response);
					break;

					case "STATE":
					if(args.length != 2)
					{
						System.out.println("Usage: java TestApp <peer_ap> STATE");
						System.exit(1);
					}

                    response = stub.state();
                    System.out.println("STATE:\n" + response);
					break;

                default: System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("TestApp exception: " + e.toString());
            e.printStackTrace();
        }
    }
}