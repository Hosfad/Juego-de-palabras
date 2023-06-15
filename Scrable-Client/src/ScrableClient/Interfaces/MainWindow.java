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
                .resize((BufferedImage) ImageUtils.getImageFromUrl("https://mir-s3-cdn-cf.behance.net/projects/404/bbf0ae95440691.Y3JvcCwxMTUwLDkwMCwyMjUsMA.jpg"), 20, 20));

        if (instance == null)
            instance = this;
        else
            throw new RuntimeException("Only one instance of MainWindow is allowed");

        setSize(750, 600);
        setLocationRelativeTo(null);

        body = new DreamPanel();
        body.setBorder(new EmptyBorder(7, 8, 7, 8));
        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        GridLayout grid = new GridLayout(0, 1);
        grid.setVgap(15);

        content.setLayout(grid);

        createButton("Crear Sala", _e -> {
            String username = JOptionPane.showInputDialog(getParent(), "Nombre de usuario");
            if (username == null)
                return;

            int port = (int)(Math.random() * 65535);
            server = new Server(port).runAsync();
            try {
                client = new Client(username, new InetSocketAddress("localhost", port)).sendConnect();
            } catch (IOException e) {
                showDialog("No se pudo conectar al servidor");
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

        createButton("Unirse a una sala", _e -> {
            JTextField gameIdField = new JTextField(), usernameField = new JTextField();
            Object[] message = { "Id de la sala :", gameIdField, "Nombre de usuario :", usernameField };

            int option = JOptionPane.showConfirmDialog(null, message, "Inicio de sesión", JOptionPane.OK_CANCEL_OPTION);
            String gameIdText = gameIdField.getText(), usernameText = usernameField.getText();

            if (option != JOptionPane.OK_OPTION)
                return;

            if (gameIdText.isEmpty() || usernameText.isEmpty()) {
                showDialog("Porfavor, rellena todos los huecos");
                return;
            }

            String[] serverIp = gameIdText.split(":");

            if (serverIp.length != 2) {
                showDialog("Id de juego invalido");
                return;
            } else if (!serverIp[1].matches("\\d+")) {
                showDialog("Puerto invalido");
                return;
            } else if (Integer.parseInt(serverIp[1]) > 65535) {
                showDialog("Puerto fuera de rango");
                return;
            }

            try {
                client = new Client(usernameText, new InetSocketAddress(serverIp[0], Integer.parseInt(serverIp[1])))
                        .sendConnect();
            } catch (NumberFormatException | IOException e) {
                showDialog("Id de juego invalido");
                return;
            }

            client.addListener(Code.JOIN, args -> {
                System.out.println("UNIRSE: " + args.message);
                if (lobbyWindow != null){
                    lobbyWindow.currentGame.addPlayer(args.data[0]);
                    lobbyWindow.redraw();
                    return;
                }

                if (args.message.equals("game_in_progress")) {
                    showDialog("Partida en progreso");
                    return;
                } else if (args.message.equals("name_taken")) {
                    showDialog("El nombre ya está en uso");
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

        createButton("Salir", _e -> {
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
