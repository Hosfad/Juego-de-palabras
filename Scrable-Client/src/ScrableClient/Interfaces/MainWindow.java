package ScrableClient.Interfaces;

import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.Game.Games;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MainWindow extends DreamFrame {

    private DreamPanel body, content;
    public MainWindow() {
        super("Juego de palabras",ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"),20,20));
        body = new DreamPanel();
        setSize(500,600);
        setLocationRelativeTo(null);
        add(body, BorderLayout.CENTER);
        body.setBorder(new EmptyBorder(7,8,7,8));


        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        GridLayout grid = new GridLayout(0,1);
        grid.setVgap(15);
        content.setLayout(grid);
        DreamButton createGame = new DreamButton("Create game");
        createGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = JOptionPane.showInputDialog(getParent() , "Username");
                if (username == null) return;
                // Create new game
                String res =  SocketClient.sendMessage("create-game," + username);
                if (res.isEmpty()) {
                    JOptionPane.showMessageDialog(getParent() , "Error: " + res);
                } else {
                    Game g = new Game(Long.parseLong(res));
                    g.addPlayer(username);
                    LobbyWindow lobbyWindow = new LobbyWindow(g);
                    lobbyWindow.currentUser = username;
                    setVisible(false);
                    lobbyWindow.setVisible(true);
                }
            }
        });


        content.add(createGame);



        DreamButton joinGame = new DreamButton("Join game");
        joinGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField gameId = new JTextField();
                JTextField username = new JPasswordField();
                Object[] message = {
                        "Game id :", gameId,
                        "Username :", username
                };

                int option = JOptionPane.showConfirmDialog(null, message, "Login", JOptionPane.OK_CANCEL_OPTION);
                if (option != JOptionPane.OK_OPTION || username.getText().isEmpty() || gameId.getText().isEmpty()) {
                    return;
                }
                String res =  SocketClient.sendMessage("join-game," + gameId.getText() + "," + username.getText());
                if (!res.equals("Name taken")){
                    Game g = new Game(Long.parseLong(res));
                    g.addPlayer(username.getText());
                    LobbyWindow lobbyWindow = new LobbyWindow(g);
                    setVisible(false);
                    lobbyWindow.currentUser = username.getText();
                    lobbyWindow.setVisible(true);
                }else {
                    JOptionPane.showMessageDialog(getParent() , "Name " + username.getText() + " is already taken"  );
                }

            }
        });
        content.add(joinGame);

    }


    public static void main(String[] args) {
        new MainWindow().setVisible(true);
    }

}
