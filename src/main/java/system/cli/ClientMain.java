package system.cli;

import system.cli.impl.ClientImpl;

public class ClientMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5555;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 5555");
            }
        }

        ClientImpl client = new ClientImpl(host, port);
        try {
            client.start();
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}