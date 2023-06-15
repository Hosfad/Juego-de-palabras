package ScrableServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ScrableServer.Client.ClientEventListener;
import ScrableServer.ServerUtils.Args;
import ScrableServer.ServerUtils.Code;

public class Server implements Runnable {
    private ServerSocket serverSocket = null;
    private List<ClientHandler> clientHandlers = new ArrayList<>();

    private HashMap<Code, Collection<ClientEventListener>> eventListeners = new HashMap<>();

    public Server(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Server started on " + getIp());
        } catch (IOException e) {
            System.out.println("Failed to start server!");
        }
    }

    public Server runAsync() {
        new Thread(this).start();
        return this;
    }

    private void onClientMessage(Args args, ClientHandler clientHandler) {
        final Code CODE = args.code;
        switch (CODE) {
            case CONNECT:
                clientHandler.name = args.data[0];
                sendMessageToClients(CODE, args.message);
                break;
            case DATA:
                for (int i = 1; i < args.data.length - 1; i+=2) {
                    String dataType = args.data[i];
                    switch (dataType) {
                        case "name":
                            String oldName = clientHandler.name;
                            clientHandler.name = args.data[i + 1];
                            sendMessageToClients(CODE, "Name changed from " + oldName + " to " + clientHandler.name);
                            break;
                        case "test":
                            sendMessageToClients(CODE, "OK!");
                            break;
                        default:
                            break;
                    }
                }
                break;
            case DISCONNECT:
                clientHandler.disconnect();
                clientHandlers.remove(clientHandler);
                sendMessageToClients(CODE, clientHandler.name);
                break;
            case MESSAGE:
                sendMessageToClients(CODE, clientHandler.name + ": " + args.message);
                break;
            default:
                System.out.println("Code: " + CODE + ", Message: " + String.join(" ", args.message));
                break;
        }
        runEvents(CODE, args);
    }

    public String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort();
        } catch (Exception e) {
            return "localhost";
        }
    }

    public void addListener(Code code, ClientEventListener listener) {
        eventListeners.computeIfAbsent(code, k -> new HashSet<>()).add(listener);
    }

    public void runEvents(Code code, Args args) {
        Collection<ClientEventListener> listeners = eventListeners.get(code);
        if (listeners == null) return;
        listeners.forEach(listener -> listener.onEvent(args));
    }

    public void sendMessageToClients(Code code, String... args) {
        System.out.println("Code: " + code + ", Message: " + String.join(" ", args));
        for (ClientHandler clientHandler : clientHandlers) {
            System.out.println("Sending message to " + clientHandler);
            clientHandler.sendMessageAsync(code, args);
        }
    }

    public void stop() {
        try {
            sendMessageToClients(Code.SHUTDOWN);
            while (!clientHandlers.isEmpty()) { Thread.sleep(100); }
            serverSocket.close();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while stopping server");
        }
    }

    @Override
    public void run() {
        while (true) {
            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
            try {
                ClientHandler clientHandler = new ClientHandler(serverSocket.accept());
                clientHandlers.add(clientHandler);

                new Thread(() -> {
                    while (!clientHandler.isClosed()) {
                        String message = clientHandler.receiveMessageAsync();
                        if (message == null)
                            continue;
                        Args args = ServerUtils.parseMessage(message);
                        onClientMessage(args, clientHandler);
                    }
                }).start();
            } catch (IOException e) {
                System.out.println("Server stopped");
                break;
            }
        }
    }
}
