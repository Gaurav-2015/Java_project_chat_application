import java.io.*;
import java.net.*;

/**
 * ChatClientConsole - console client.
 * Usage: java -cp out ChatClientConsole [serverIP] [port]
 */
public class ChatClientConsole {
    public static void main(String[] args) {
        String server = args.length>=1 ? args[0] : "127.0.0.1";
        int port = args.length>=2 ? Integer.parseInt(args[1]) : 1234;

        try (Socket socket = new Socket(server, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.print("Enter your name: ");
            String name = sysIn.readLine();
            if (name == null || name.trim().isEmpty()) name = "User" + (int)(Math.random()*1000);
            out.println(name);

            Thread reader = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) System.out.println(msg);
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            reader.start();

            String line;
            while ((line = sysIn.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                out.println(line);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}
