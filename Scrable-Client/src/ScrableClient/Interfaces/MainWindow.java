package ScrableClient.Interfaces;

import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.ServerResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class MainWindow extends DreamFrame {

    private DreamPanel body, content;

    public MainWindow() {
        super("Juego de palabras", ImageUtils
                .resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"), 20, 20));
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
            Container ctx = getParent();
            String username = JOptionPane.showInputDialog(ctx, "Username");
            if (username == null)
                return;
            // Create new game
            String res = SocketClient.sendMessage("create-game," + username);
            if (res.startsWith("OK:")) {
                res = res.substring(3);
                Game g = new Game(Long.parseLong(res));
                g.addPlayer(username);

                LobbyWindow lobbyWindow = new LobbyWindow(g , username);

                setVisible(false);
                lobbyWindow.setVisible(true);
                return;

            }
            JOptionPane.showMessageDialog(ctx, "Error: " + res);
        });

        createButton("Join Game", _e -> {
            JTextField gameId = new JTextField(), username = new JTextField();
            String gameIdText = gameId.getText(), usernameText = username.getText();
            Object[] message = {"Game id :", gameId, "Username :", username};

            int option = JOptionPane.showConfirmDialog(null, message, "Login", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION || gameIdText.isEmpty() || usernameText.isEmpty())
                return;

            String response = SocketClient.sendMessage("join-game," + gameIdText + "," + usernameText);
            if (response != null && response.startsWith("OK:")) {
                // Start new game
                Game g = new Game(Long.parseLong(response.substring(3)));
                g.addPlayer(usernameText);
                LobbyWindow lobbyWindow = new LobbyWindow(g,usernameText);
                lobbyWindow.setVisible(true);
                setVisible(false);

            }
            ServerResponse res = ServerResponse.valueOf(response.split(":")[0]);
            switch (res) {
                case GAME_ALREADY_STARTED -> showDialog("Game " + gameIdText + " already started");
                case NAME_TAKEN -> showDialog("Name " + usernameText + " is already taken");
                default -> JOptionPane.showMessageDialog(getParent(), "Error: " + response);
            }
        });

        createButton("Exit", _e -> System.exit(0));
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
