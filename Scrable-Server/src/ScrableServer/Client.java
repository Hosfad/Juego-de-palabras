package ScrableServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import ScrableServer.ServerUtils.Args;
import ScrableServer.ServerUtils.Code;

public class Client {
    public String name;

    private DataInputStream in;
    private DataOutputStream out;
    private Socket clientSocket;

    private HashMap<Code, Collection<ClientEventListener>> eventListeners = new HashMap<>();

    public interface ClientEventListener {
        void onEvent(Args args);
    }

    // Chat to test networking (Run server first)
    public static void main(String[] args) {
        String serverName = "localhost";
        int port = 6969;

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your name: ");
        Client client = new Client(scanner.nextLine(), serverName, port);

        System.out.println("""

                Feel free to send messages to the server!
                Use: data <arg_name> <message> to send data to the server
                Use: help for arguments available
                Use: exit to disconnect from the server
                """);

        while (!client.isClosed()) {
            String message = scanner.nextLine();
            String[] messageArgs = message.split(" ");

            switch (messageArgs[0]) {
                case "data":
                    if (messageArgs.length % 2 == 0) {
                        System.out.println("Not enough arguments!");
                        break;
                    }
                    client.sendMessageToServer(Code.DATA, messageArgs);
                    break;
                case "help":
                    System.out.println("""
                            Available arguments:
                            data <arg_name> <message> - send data to the server
                                args:
                                    name - change your name
                                    test - test server response
                            help - show this message
                            exit - disconnect from the server
                            """);
                    break;
                case "exit":
                    client.disconnect();
                    break;
                default:
                    client.sendMessageToServer(Code.MESSAGE, message);
                    break;
            }
        }

        scanner.close();
    }

    public Client(String name, String ip, int port) {
        this.name = name;
        try {
            System.out.println("Connecting to " + ip + " on port " + port);
            clientSocket = new Socket(ip, port);
            System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            // Default Events
            addListener(Code.CONNECT, System.out::println);
            addListener(Code.MESSAGE, System.out::println);
            addListener(Code.DISCONNECT, System.out::println);
            addListener(Code.DATA, msgArgs -> System.out.println("Data: " + msgArgs.data[0] + " " + msgArgs.data[1]));
            addListener(Code.SHUTDOWN, msgArgs -> sendMessageToServer(Code.DISCONNECT, name));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> disconnect()));

            new Thread(() -> {
                while (!isClosed()) {
                    try {
                        Args args = ServerUtils.parseMessage(in.readUTF());
                        runEvents(args.code, args);
                    } catch (IOException e) {
                        System.out.println("Error receiving message from server!");
                    }
                }
            }).start();
        
            connect();
        } catch (IOException e) {
            System.out.println("Error connecting to server!");
        }
    }

    public void sendMessageToServer(Code code, String... args) {
        try {
            out.writeUTF(code.ordinal() + " " + String.join(" ", args));
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending message to server!");
        }
    }

    public void sendMessageToServer(Code code) {
        sendMessageToServer(code, code.toString());
    }

    public boolean isClosed() {
        return clientSocket.isClosed();
    }

    public void addListener(Code code, ClientEventListener listener) {
        eventListeners.computeIfAbsent(code, k -> new HashSet<>()).add(listener);
    }

    public void runEvents(Code code, Args args) {
        Collection<ClientEventListener> listeners = eventListeners.get(code);
        if (listeners == null) return;
        listeners.forEach(listener -> listener.onEvent(args));
    }

    public Client connect() {
        sendMessageToServer(Code.CONNECT, name);
        return this;
    }

    public void disconnect() {
        try {
            sendMessageToServer(Code.DISCONNECT, name);
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error disconnecting from server!");
        }
    }
}
