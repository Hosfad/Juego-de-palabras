package ScrableServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

public class ClientHandler {
    private DataOutputStream out;
    private DataInputStream in;
    private Socket clientSocket;
    public String name;

    public ClientHandler(Socket socket) {
        try {
            clientSocket = socket;
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receiveMessageAsync() {
        try {
            return in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessageAsync(Code code, String... message) {
        try {
            out.writeUTF(code.ordinal() + " " + String.join(" ", message));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isClosed() {
        return clientSocket.isClosed();
    }

    public void disconnect() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SocketAddress getAddress() {
        return clientSocket.getRemoteSocketAddress();
    }

    @Override
    public String toString() {
        return name + ":" + getAddress();
    }
}
