package system.cli;

import system.cli.impl.ServerImpl;

public class ServerMain {
    public static void main(String[] args) {
        int port = 5555;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 5432");
            }
        }

        ServerImpl server = new ServerImpl(port);
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}