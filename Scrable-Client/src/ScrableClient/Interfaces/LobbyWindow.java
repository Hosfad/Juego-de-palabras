package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.Game.Games;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LobbyWindow extends DreamFrame {

    private DreamPanel body, content;
    Image icon = ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"),20,20);
    Gson gson = new Gson();
    Game currentGame;
    public String currentUser;
    Thread pollingThread;

    public LobbyWindow(Game game) {
        super("Sala de espera",ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"),20,20));
         body = new DreamPanel();
         this.currentGame = game;
        setSize(500,600);
        setLocationRelativeTo(null);
        add(body, BorderLayout.CENTER);
        body.setBorder(new EmptyBorder(7,8,7,8));


        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        GridLayout grid = new GridLayout(0,2);
        grid.setVgap(15);
        content.setLayout(grid);
        content.add(new DreamLabel("Game id: "));
        DreamTextField f = new DreamTextField();
        f.setText(game.id + "");
        f.setEditable(false);
        content.add(f);
        content.add(new DreamLabel("Status: ") );
        content.add(new DreamLabel( currentGame.gameState.name()));
        content.add(new DreamLabel("Connected users: ") );
        content.add(new DreamLabel( currentGame.players.size()+ ""));
        DreamButton readyButton = new DreamButton("Ready");
        readyButton.addActionListener(e->{
            String res = SocketClient.sendMessage("player-ready," + game.id + "," + currentUser);
            if(res.equals("ok")){
                content.remove(readyButton);
            }
        });
        content.add(readyButton);

        DreamButton leaveGame = new DreamButton("Leave Game");
        leaveGame.addActionListener(e->{
            String res = SocketClient.sendMessage("leave-game," + game.id + "," + currentUser);
                pollingThread.stop();
                pollingThread.interrupt();
                this.setVisible(false);
                this.dispose();
                new MainWindow().setVisible(true);
        });
        content.add(leaveGame);


        List playerList = new List();
        playerList.setForeground(Color.white);
        playerList.setBackground(UIColours.BODY_COLOUR);

        pollingThread = new Thread(()->{
            while (true){
               String res= SocketClient.sendMessage("get-game-info," + game.id + "," + currentUser  );
                System.out.println(res);
                Game g = gson.fromJson(res, Game.class);
                this.currentGame = g;
                playerList.removeAll();
                for (Game.Player player : g.players) {
                    String ready = player.isReady ? "(Ready)" : "";
                    playerList.add("Player : " + player.name + " " + ready);
                }

                if (g.gameState == Game.State.IN_PROGRESS){
                    JOptionPane.showInputDialog("Game starting in " +(int) ((g.startTime - System.currentTimeMillis()) / 1000)  + " seconds");
                    // Start new game
                    if (System.currentTimeMillis() > g.startTime){




                        this.setVisible(false);
                        this.dispose();
                        GameWindow gameWindow = new GameWindow(g , currentUser);
                        gameWindow.setVisible(true);
                        pollingThread.interrupt();
                        pollingThread.stop();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        DreamScrollPane dsp = new DreamScrollPane(playerList);
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        body.add(dsp);
        pollingThread.start();

    }


    public static void main(String[] args) throws IOException {
        LobbyWindow window = new LobbyWindow(Games.createNewGame());
        window.setVisible(true);
    }

}
