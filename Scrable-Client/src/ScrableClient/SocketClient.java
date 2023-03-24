package ScrableClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.JOptionPane;

public class SocketClient {
  private static Socket socket = null;
  
  private static DataOutputStream out = null;
  
  static String address = "127.0.0.1";
  
  static int port = 5000;

  public static String sendMessage(String message) {
    return sendMessageAsync(message);
  }
  
  private static String sendMessageAsync(String message) {
    try {
      socket = new Socket(address, port);
      out = new DataOutputStream(socket.getOutputStream());
      message = message + "\n";
      out.write(message.getBytes(StandardCharsets.UTF_8));
      ClientListener clientListener = new ClientListener();
        Thread thread = new Thread(clientListener);
        thread.start();
        thread.join();
      System.out.println("Message sent");
      return clientListener.serverResponse;
    } catch (IOException u) {
      System.out.println(u);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return "Server failed to respond";
  }
  
  static class ClientListener implements Runnable {
    public String serverResponse ;
    public void run() {
      try {
        DataInputStream dataIn = new DataInputStream(SocketClient.socket.getInputStream());
        serverResponse = dataIn.readUTF();
      } catch (Exception except) {
        System.out.println("Error in Writer--> " + except.getMessage());
        except.printStackTrace();
      } 
    }
  }
}