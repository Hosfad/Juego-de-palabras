package ScrableServer.SocketServer;

import ScrableServer.Game.Game;
import ScrableServer.Game.Games;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
    private Socket socket = null;

    private ServerSocket server = null;

    private DataInputStream in = null;

    public static void main(String[] args) {
        try {
            SocketServer server = new SocketServer(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SocketServer(int port) throws IOException {
        this.server = new ServerSocket(port);
        while (true) {
            try {
                while (true) {
                    System.out.println("Server started, waiting for client...");
                    socket = server.accept();
                    handleMessage();
                }
            } catch (IOException i) {
                System.out.println(i);
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Send a response to client
     *
     * @param message The message to send
     * @throws IOException
     * @see SocketServer#handleMessage() must be used before socket is closed or will throw an error
     */
    private void respondToClient(String message) throws IOException {
        DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
        dos.writeUTF(message);
    }

    private void handleMessage() {
        (new Thread() {
            public void run() {
                try {
                    String partyId, userName;
                    InputStreamReader reader = new InputStreamReader(SocketServer.this.socket.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    String msg = bufferedReader.readLine();
                    String[] msgParts = msg.split(",");
                    String command = msgParts[0];
                    String gameId;
                    Game game;
                    switch (command) {
                        case "get-game-info":
                            gameId = msgParts[1];
                            game = Games.getGame(g -> g.id == Long.parseLong(gameId));
                            if (game == null) {
                                respondToClient("404 not found");
                            } else {
                                System.out.println(game.toString());
                                Game.Player p = game.getPlayer(p1-> p1.name.equals(msgParts[2]));
                                if (p != null) {
                                    p.lastPing = System.currentTimeMillis();
                                }
                                respondToClient(game.toString());
                            }
                            break;
                        case "join-game":
                            gameId = msgParts[1];
                            game = Games.getGame(g -> g.id == Long.parseLong(gameId));
                            if (game == null) {
                                respondToClient("404 not found");
                            } else {
                                if (game.addPlayer(msgParts[2])) {
                                respondToClient(gameId);
                                }else {
                                    respondToClient("Name taken");
                                }
                            }
                            break;
                        case "leave-game":
                            System.out.println("leave-game" + msg);
                            gameId = msgParts[1];
                            game = Games.getGame(g -> g.id == Long.parseLong(gameId));
                            if (game == null) {
                                System.out.println("404 not found");
                                respondToClient("404 not found");
                            } else {
                                if (msgParts.length > 2){
                                    game.removePlayer(msgParts[2]);
                                    if (game.players.size() == 0) {
                                        Games.removeGame(g -> g.id == Long.parseLong(gameId));
                                    }
                                }else {
                                    Games.removeGame(g -> g.id == Long.parseLong(gameId));
                                    System.out.println("Game ended because all players left");
                                }
                                respondToClient("ok");

                            }
                            break;
                        case "player-ready":
                            gameId = msgParts[1];
                            game = Games.getGame(g -> g.id == Long.parseLong(gameId));
                            if (game == null) {
                                respondToClient("404 not found");
                            } else {
                                Game.Player p = game.getPlayer(pl -> pl.name.equals(msgParts[2]));
                                if (p == null) {
                                    respondToClient("404 not found");
                                } else {
                                    p.lastPing = System.currentTimeMillis();
                                    p.isReady = true;
                                    respondToClient("ok");
                                }
                            }
                            break;
                        case "create-game":
                            String hostName = msgParts[1];
                            Game g = Games.createNewGame();
                            g.addPlayer(hostName);
                            respondToClient(g.id + "");

                            break;
                    }

                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).run();
    }
}
