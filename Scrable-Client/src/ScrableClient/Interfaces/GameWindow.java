package ScrableClient.Interfaces;

import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.GameListener.GameListener;
import ScrableClient.GameListener.GameTick;
import ScrableClient.Main;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.ServerResponse;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class GameWindow extends DreamFrame implements GameListener {

    private DreamPanel body, content;
    Image icon = ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"),20,20);
    Gson gson = new Gson();
    Game currentGame;
    public String currentUserId;
    JTable usersTable = new JTable();

    public GameWindow(Game game , String currentUserId ) {
        super("Juego de palabras",ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"),20,20));
        body = new DreamPanel();
        this.currentGame = game;
        this.currentUserId = currentUserId;
        setSize(500,600);
        setLocationRelativeTo(null);
        add(body, BorderLayout.CENTER);
        body.setBorder(new EmptyBorder(7,8,7,8));

        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        GridLayout grid = new GridLayout(0,1);
        grid.setVgap(15);
        content.setLayout(grid);

        content.add(new DreamLabel("Game started"));
        usersTable.setModel(new DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                        "User", "Score"
                }));
        JScrollPane scrollPane = new JScrollPane(usersTable);
        content.add(scrollPane);

    }



    @Override
    public void onGameTick(GameTick event) {

        if (event.status == ServerResponse.GAME_NOT_FOUND){
            JOptionPane.showMessageDialog(null, "Game not found");
            this.dispose();
            Main.mainWindow.setVisible(true);
            return;
        }
        if (event.status == ServerResponse.GAME_ENDED){
            JOptionPane.showMessageDialog(null, "Game ended");
            this.dispose();
            Main.mainWindow.setVisible(true);
            return;
        }

    }

    @Override
    public void onExit() {
        String res = SocketClient.sendMessage("leave-game," + getGameId() + "," + getUserId());
        this.setVisible(false);
        this.dispose();
        Main.mainWindow.setVisible(true);
        Main.removeListener(Main.getInstance(this.getClass()));
    }
    @Override
    public String getGameId() {
        return currentGame.id +"";
    }

    @Override
    public String getUserId() {
        return currentUserId;
    }


}
