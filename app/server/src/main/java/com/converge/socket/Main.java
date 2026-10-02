package com.converge.socket;

import com.converge.socket.core.SocketServer;
import com.converge.socket.handler.math.MathOperationHandler;
import com.converge.socket.handler.math.MathOperationRequest;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        int port = SocketServer.DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Expected usage: java -jar server.jar [PORT]");
                return;
            }
        }

        SocketServer server = new SocketServer(port);

        // Register custom business handlers


        try {
            server.start();
            System.out.println("Socket Server is running on port " + port);
            System.out.println("Connected clients can execute registered business handlers.");
            System.out.println("To stop the server gracefully, type 'stop', 'exit' or 'desativar'.\n");
        } catch (Exception e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
            return;
        }

        // Console command loop for administrative shutdown
        Scanner scanner = new Scanner(System.in);
        while (server.isRunning()) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String command = scanner.nextLine().trim().toLowerCase();

            if (command.equals("stop") || command.equals("exit")) {
                System.out.println("Shutting down server...");
                server.stop();
                System.out.println("Server successfully stopped.");
                break;
            } else if (!command.isEmpty()) {
                System.err.println("Unknown command: '" + command + ". Use 'stop' to shut down.");
            }
        }
    }
}
