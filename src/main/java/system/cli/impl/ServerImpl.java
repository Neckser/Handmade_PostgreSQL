package system.cli.impl;

import system.cli.api.BackendWorker;
import system.cli.api.Engine;
import system.cli.api.Server;
import system.cli.impl.BackendWorkerImpl;
import system.cli.impl.EngineImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerImpl implements Server {
    private final int port;
    private final Engine engine;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public ServerImpl(int port) {
        this.port = port;
        this.engine = new EngineImpl();
    }

    public ServerImpl(int port, Engine engine) {
        this.port = port;
        this.engine = engine;
    }

    @Override
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                BackendWorker worker = new BackendWorkerImpl(clientSocket, engine);

                Thread clientThread = new Thread(worker);
                clientThread.setDaemon(true);
                clientThread.start();

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
        }
    }
}
