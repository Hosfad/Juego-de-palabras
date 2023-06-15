package ScrableServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

    public Client(String name, InetSocketAddress socketAddress) throws IOException {
        this.name = name;

        System.out.println("Connecting to " + socketAddress.getAddress() + " on port " + socketAddress.getPort());
        clientSocket = new Socket();
        clientSocket.connect(socketAddress, 2000);
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
        if (listeners == null)
            return;
        listeners.forEach(listener -> listener.onEvent(args));
    }

    public Client sendConnect() {
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
