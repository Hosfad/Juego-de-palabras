package ScrableClient.Interfaces;

import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.Game.Game;
import ScrableServer.ServerUtils.Code;
import ScrableServer.Client;
import ScrableServer.Server;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MainWindow extends DreamFrame {
    public static MainWindow instance = null;

    public Server server;
    public Client client;
    public LobbyWindow lobbyWindow;
    private DreamPanel body, content;

    public MainWindow() {
        super("Juego de palabras", ImageUtils
                .resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"), 20, 20));

        if (instance == null)
            instance = this;
        else
            throw new RuntimeException("Only one instance of MainWindow is allowed");

        setSize(500, 600);
        setLocationRelativeTo(null);

        body = new DreamPanel();
        body.setBorder(new EmptyBorder(7, 8, 7, 8));
        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        GridLayout grid = new GridLayout(0, 1);
        grid.setVgap(15);

        content.setLayout(grid);

        createButton("Create Game", _e -> {
            String username = JOptionPane.showInputDialog(getParent(), "Username");
            if (username == null)
                return;

            int port = (int) (Math.random() * 65535);
            server = new Server(port).runAsync();
            try {
                client = new Client(username, new InetSocketAddress("localhost", port)).sendConnect();
            } catch (IOException e) {
                showDialog("Couldn't connect to server");
                return;
            }

            Game game = new Game(server.getIp());
            lobbyWindow = new LobbyWindow(game, username);

            setVisible(false);
            lobbyWindow.setVisible(true);

            client.addListener(Code.JOIN, args -> {
                if (args.message.equals("game_in_progress") || args.message.equals("name_taken")) {
                    return;
                }
                game.addPlayer(args.data[0]);
                lobbyWindow.redraw();
            });

            client.sendMessageToServer(Code.JOIN, username);
        });

        createButton("Join Game", _e -> {
            JTextField gameIdField = new JTextField(), usernameField = new JTextField();
            Object[] message = { "Game id :", gameIdField, "Username :", usernameField };

            int option = JOptionPane.showConfirmDialog(null, message, "Login", JOptionPane.OK_CANCEL_OPTION);
            String gameIdText = gameIdField.getText(), usernameText = usernameField.getText();

            if (option != JOptionPane.OK_OPTION)
                return;

            if (gameIdText.isEmpty() || usernameText.isEmpty()) {
                showDialog("Please fill all the fields");
                return;
            }

            String[] serverIp = gameIdText.split(":");

            if (serverIp.length != 2) {
                showDialog("Invalid game id");
                return;
            } else if (!serverIp[1].matches("\\d+")) {
                showDialog("Invalid port");
                return;
            } else if (Integer.parseInt(serverIp[1]) > 65535) {
                showDialog("Port out of range");
                return;
            }

            try {
                client = new Client(usernameText, new InetSocketAddress(serverIp[0], Integer.parseInt(serverIp[1])))
                        .sendConnect();
            } catch (NumberFormatException | IOException e) {
                showDialog("Invalid game id");
                return;
            }

            client.addListener(Code.JOIN, args -> {
                if (lobbyWindow != null){
                    lobbyWindow.currentGame.addPlayer(args.data[0]);
                    lobbyWindow.redraw();
                    return;
                }

                if (args.message.equals("game_in_progress")) {
                    showDialog("Game in progress");
                    return;
                } else if (args.message.equals("name_taken")) {
                    showDialog("Name already taken");
                    return;
                }

                System.out.println("JOIN: " + args.message);
                Game game = new Game(gameIdText);
                for (int i = 0, j = 0; i < args.data.length - 1; i += 2, j++) {
                    game.addPlayer(args.data[i]);
                    game.players.get(j).isReady = Boolean.parseBoolean(args.data[i + 1]);
                }

                lobbyWindow = new LobbyWindow(game, usernameText);
                lobbyWindow.setVisible(true);
                setVisible(false);
            });

            client.sendMessageToServer(Code.JOIN, usernameText);
        });

        createButton("Exit", _e -> {
            disconnect();
            System.exit(0);
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                disconnect();
            }
        });
    }

    public void disconnect() {
        if (client != null)
            client.disconnect();
        if (server != null)
            server.stop();
        lobbyWindow = null;
    }

    public void showDialog(String message) {
        JOptionPane.showMessageDialog(getParent(), message);
    }

    public void createButton(String text, ActionListener listener) {
        DreamButton button = new DreamButton(text);
        button.addActionListener(listener);
        content.add(button);
    }

}
