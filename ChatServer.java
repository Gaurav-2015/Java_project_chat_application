import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ChatServer: accepts multiple clients, each handled by a thread,
 * broadcasts messages to all connected clients.
 */
public class ChatServer {
    public static final int PORT = 1234;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        System.out.println("ChatServer starting on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                handler.start();
                System.out.println("New client: " + socket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler c : clients) {
            if (c != exclude) c.sendMessage(message);
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name = "Anonymous";

        ClientHandler(Socket socket) { this.socket = socket; }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String first = in.readLine();
                if (first != null && !first.trim().isEmpty()) name = first.trim();
                broadcast(name + " has joined the chat.", this);

                String msg;
                while ((msg = in.readLine()) != null) {
                    String full = name + ": " + msg;
                    System.out.println(full);
                    broadcast(full, this);
                }
            } catch (IOException e) {
                System.out.println("Connection lost: " + socket.getRemoteSocketAddress());
            } finally {
                try {
                    removeClient(this);
                    broadcast(name + " has left the chat.", this);
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ignored) {}
            }
        }

        void sendMessage(String message) {
            if (out != null) out.println(message);
        }
    }
}
