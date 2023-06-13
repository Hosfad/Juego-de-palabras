package ScrableServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

enum Code {
    DISCONNECT,
    CONNECT,
    MESSAGE,
    DATA
}

public class Client {
    DataInputStream in;
    DataOutputStream out;
    Socket clientSocket;

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

        while (client.isConnected()) {
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
                    client.sendMessageToServer(Code.DISCONNECT);
                    break;
                default:
                    client.sendMessageToServer(Code.MESSAGE, message);
                    break;
            }
        }

        scanner.close();
        client.disconnect();
    }

    public Client(String name, String ip, int port) {
        try {
            System.out.println("Connecting to " + ip + " on port " + port);
            clientSocket = new Socket(ip, port);
            System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> disconnect()));

            new Thread(() -> {
                while (clientSocket.isConnected()) {
                    try {
                        onServerMessage(in.readUTF());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            sendMessageToServer(Code.CONNECT, "name", name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onServerMessage(String codedMessage) {
        final Args ARGS = ServerUtils.parseMessage(codedMessage);

        switch (ARGS.code) {
            case DISCONNECT:
                break;
            case CONNECT:
                System.out.println(ARGS.message);
                break;
            case MESSAGE:
                System.out.println(ARGS.message);
                break;
            case DATA:
                System.out.println("Data: " + ARGS.data[0] + " " + ARGS.data[1]);
                break;
            default:
                break;
        }
    }

    public void sendMessageToServer(Code code, String... args) {
        try {
            out.writeUTF(code.ordinal() + " " + String.join(" ", args));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToServer(Code code) {
        sendMessageToServer(code, code.toString());
    }

    public boolean isConnected() {
        return clientSocket.isConnected();
    }

    public void disconnect() {
        try {
            sendMessageToServer(Code.DISCONNECT);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
