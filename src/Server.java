import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final String REGISTER = "REGISTER";
    private static final String LOOKUP = "LOOKUP";

    private static final String PLATE_REGEX = "[0-9A-Z]{2}-[0-9A-Z]{2}-[0-9A-Z]{2}";

    private static Map<String, String> plates = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java Server <port_number>");
            System.exit(-1);
        }

        DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));

        while (true) {
            byte[] receiveData = new byte[1024], sendData;

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String answer = parseRequest(new String(receivePacket.getData()).trim());

            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            sendData = answer.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
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
