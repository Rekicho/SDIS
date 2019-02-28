import java.net.*;

public class Client {
    private static final String REGISTER = "register";
    private static final String LOOKUP = "lookup";

    private static final String ALREADY_REGISTERED = "-1";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String ERROR = "ERROR";

    public static void main(String[] args) throws Exception {
        if ((args.length == 4 && args[2].equals(LOOKUP)) || (args.length == 5 && args[2].equals(REGISTER))) {
            System.out.println("Usage: java Client <host_name> <port_number> <oper> <opnd>*");
            System.out.println("       java Client <host_name> <port_number> register <plate number> <owner name>");
            System.out.println("       java Client <host_name> <port_number> lookup <plate number>");
            System.exit(-1);
        }

        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(args[0]);

        byte[] sendData, receiveData = new byte[1024];

        String request = args[2].equals(LOOKUP) ?
                         args[2].toUpperCase() + " " + args[3] :
                         args[2].toUpperCase() + " " + args[3] + " " + args[4];
        sendData = request.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.parseInt(args[1]));
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        String response = new String(receivePacket.getData()).trim();

        System.out.println(request + ": " + (response.equals(ALREADY_REGISTERED) || response.equals(NOT_FOUND) ? ERROR : response));

        clientSocket.close();
    }
}
