package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import com.google.gson.Gson;

import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class GameWindow extends DreamFrame {

    private DreamPanel body, content;
    Image icon = ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"),20,20);
    Gson gson = new Gson();
    Game currentGame;
    public String currentUserId;
    Thread pollingThread;

    public GameWindow(Game game ,String currentUserId ) {
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
        List playersList = new List();
        playersList.setBackground(UIColours.BODY_COLOUR);
        playersList.setForeground(Color.white);
        for (Game.Player player : game.players) {
            playersList.add(player.name);
        }
        content.add(playersList);

        pollingThread = new Thread(()->{
            while (true){
                try {
                    String res= SocketClient.sendMessage("get-game-info," + game.id + "," + currentUserId  );
                    if (res.equals("null")){
                        pollingThread.stop();
                        pollingThread.interrupt();
                        this.setVisible(false);
                        this.dispose();
                        new MainWindow().setVisible(true);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                String res = SocketClient.sendMessage("leave-game," + game.id + "," + currentUserId);
                pollingThread.stop();
                pollingThread.interrupt();
                setVisible(false);
                dispose();
                new MainWindow().setVisible(true);
            }
        });

        pollingThread.start();
    }


    public static void main(String[] args) throws IOException {
    }

}
