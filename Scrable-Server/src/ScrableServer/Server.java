package ScrableServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

class Args {
    public Code code;
    public String message;
    public String[] data;

    public Args(Code code, String message) {
        this.code = code;
        this.message = message;
        this.data = message.split(" ");
    }
}

class ServerUtils {
    public static Args parseMessage(String args) {
        String[] values = args.split(" ");
        Code code = Code.values()[Integer.parseInt(values[0])];
        args = args.substring(values[0].length() + 1);
        return new Args(code, args);
    }
}

public class Server {
    private ServerSocket serverSocket = null;
    private List<ClientHandler> clientHandlers = new ArrayList<>();

    public static void main(String[] args) {
        new Server(6969);
    }

    public Server(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Server started on " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
            while (true) {
                System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");

                ClientHandler clientHandler = new ClientHandler(serverSocket.accept());
                clientHandlers.add(clientHandler);

                new Thread(() -> {
                    while (!clientHandler.isClosed()) {
                        String args = clientHandler.receiveMessageAsync();
                        onClientMessage(args, clientHandler);
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the message from the client and calls the appropriate method
     * 
     * @param codedMessage  The message from the client
     * @param clientHandler The client that sent the message
     * 
     *                      Code DISCONNECT: Removes the client from the list of
     *                      clients and sends a message to all clients that the
     *                      client disconnected
     *                      Code CONNECT: Receives and sets the name of the client
     *                      and sends a message to all clients that the client
     *                      connected
     *                      Code MESSAGE: Sends a message to all clients
     *                      Code DATA: Parses the data type and sends the data to
     *                      all clients
     */
    private void onClientMessage(String codedMessage, ClientHandler clientHandler) {
        if (codedMessage == null)
            return;
        final Args ARGS = ServerUtils.parseMessage(codedMessage);
        final Code CODE = ARGS.code;
        switch (CODE) {
            case CONNECT:
                clientHandler.name = ARGS.data[1];
                sendMessageToClients(CODE, clientHandler + " connected");
                break;
            case DATA:
                for (int i = 1; i < ARGS.data.length - 1; ++i) {
                    String dataType = ARGS.data[i];
                    switch (dataType) {
                        case "name":
                            String oldName = clientHandler.name;
                            clientHandler.name = ARGS.data[i + 1];
                            sendMessageToClients(CODE, "Name changed from " + oldName + " to " + clientHandler.name);
                            break;
                        case "test":
                            sendMessageToClients(CODE, "Test successful!");
                            break;
                        default:
                            break;
                    }
                }
                break;
            case DISCONNECT:
                clientHandler.disconnect();
                clientHandlers.remove(clientHandler);
                sendMessageToClients(CODE, clientHandler + " disconnected");
                break;
            case MESSAGE:
                sendMessageToClients(CODE, clientHandler.name + ": " + ARGS.message);
                break;
            default:
                break;
        }
    }

    private void sendMessageToClients(Code code, String... args) {
        System.out.println("Code: " + code + ", Message: " + String.join(" ", args));
        for (ClientHandler clientHandler : clientHandlers) {
            System.out.println("Sending message to " + clientHandler);
            clientHandler.sendMessageAsync(code, args);
        }
    }
}
