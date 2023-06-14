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

public class MainWindow extends DreamFrame {
    public static MainWindow instance = null;

    public Server server;
    public Client client;
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
            
            int port = (int)(Math.random() * 65535);
            server = new Server(port).runAsync();
            client = new Client(username, "localhost", port).connect();

            Game game = new Game(server.getIp());
            game.addPlayer(username);

            LobbyWindow lobbyWindow = new LobbyWindow(game, username);

            setVisible(false);
            lobbyWindow.setVisible(true);
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

            // TODO: Handle wrong ip error
            String[] serverIp = gameIdText.split(":");
            client = new Client(usernameText, serverIp[0], Integer.parseInt(serverIp[1])).connect();

            client.addListener(Code.JOIN, args -> {
                System.out.println("JOIN: " + args.message);
                Game game = new Game(gameIdText);
                for (int i = 0, j = 0; i < args.data.length - 1; i += 2, j++) {
                    game.addPlayer(args.data[i]);
                    game.players.get(j).isReady = Boolean.parseBoolean(args.data[i + 1]);
                }
                LobbyWindow lobbyWindow = new LobbyWindow(game, usernameText);
                lobbyWindow.setVisible(true);
                setVisible(false);
            });

            client.sendMessageToServer(Code.JOIN);

            // TODO: add logic for game not existing and showing error dialog
            // TODO: add logic game started
            // TODO: add logic name_taken
        });

        createButton("Exit", _e -> {
            disconnect();
            System.exit(0);
        });
    }

    public void disconnect() {
        if (client != null)
            client.disconnect();
        if (server != null)
            server.stop();
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
