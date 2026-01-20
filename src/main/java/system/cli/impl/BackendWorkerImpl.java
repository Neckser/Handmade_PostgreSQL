package system.cli.impl;

import system.cli.api.BackendWorker;
import system.cli.api.Engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class BackendWorkerImpl implements BackendWorker {
    private final Socket clientSocket;
    private final Engine engine;

    public BackendWorkerImpl(Socket clientSocket, Engine engine) {
        this.clientSocket = clientSocket;
        this.engine = engine;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            out.println("Welcome to T-Bank Database CLI!");
            out.println("Type 'help' for available commands, 'exit' or 'quit' to disconnect.");
            out.println("END");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim();

                if (inputLine.equalsIgnoreCase("exit") || inputLine.equalsIgnoreCase("quit")) {
                    out.println("Connection closed.");
                    out.println("END");
                    break;
                }

                if (inputLine.isEmpty()) {
                    out.println("END");
                    continue;
                }

                String result = engine.executeSql(inputLine);

                String[] lines = result.split("\n");
                for (String line : lines) {
                    out.println(line);
                }

                out.println("END");
            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
            }
        }
    }
}