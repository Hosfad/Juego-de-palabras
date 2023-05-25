package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.Game.Games;
import com.google.gson.Gson;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class LobbyWindow extends DreamFrame {

    String currentUser;
    Gson gson = new Gson();
    Game currentGame;
    Thread pollingThread;
    DreamLabel connectedUsersNum = new DreamLabel("0");
    Image icon = ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"), 20,
            20);

    private DreamPanel body, content;

    public LobbyWindow(Game game) {
        super("Sala de espera", ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"), 20, 20));
        setSize(500, 600);
        setLocationRelativeTo(null);

        currentGame = game;

        body = new DreamPanel();
        body.setBorder(new EmptyBorder(7, 8, 7, 8));
        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        GridLayout grid = new GridLayout(0, 2);
        grid.setVgap(15);

        content.setLayout(grid);
        content.add(new DreamLabel("Game id: "));

        DreamTextField f = new DreamTextField();

        f.setText(game.id + "");
        f.setEditable(false);

        content.add(f);
        addContentLabel("Status: ");
        addContentLabel(currentGame.gameState.name());
        addContentLabel("Connected users: ");
        connectedUsersNum.setText(currentGame.players.size() + "");
        content.add(connectedUsersNum);

        createButton("Ready", "start.png", e -> {
            String res = SocketClient.sendMessage("player-ready," + game.id + "," + currentUser);
            if (res.equals("ok")) content.remove(this);
        }, 20, 20);

        createButton("Leave Game", "stop.png", e -> {
            SocketClient.sendMessage("leave-game," + game.id + "," + currentUser);
            pollingThread.interrupt();
            this.setVisible(false);
            this.dispose();
            new MainWindow().setVisible(true);
        }, 20, 20);

        List playerList = new List();
        playerList.setForeground(Color.white);
        playerList.setBackground(UIColours.BODY_COLOUR);

        pollingThread = new Thread(() -> {
            while (true) {
                String res = SocketClient.sendMessage("get-game-info," + game.id + "," + currentUser);
                if (res.equals("null")) {
                    pollingThread.interrupt();
                    this.setVisible(false);
                    this.dispose();
                    new MainWindow().setVisible(true);
                }
                System.out.println(res);
                Game g = gson.fromJson(res, Game.class);
                this.currentGame = g;
                playerList.removeAll();
                for (Game.Player player : g.players) {
                    String ready = player.isReady ? "(Ready)" : "";
                    playerList.add("Player : " + player.name + " " + ready);
                }
                connectedUsersNum.setText(g.players.size() + "");

                if (g.gameState == Game.State.IN_PROGRESS) {
                    JOptionPane.showInputDialog("Game starting in "
                            + (int) ((g.startTime - System.currentTimeMillis()) / 1000) + " seconds");
                    // Start new game
                    if (System.currentTimeMillis() > g.startTime) {

                        this.setVisible(false);
                        this.dispose();
                        GameWindow gameWindow = new GameWindow(g, currentUser);
                        gameWindow.setVisible(true);
                        pollingThread.interrupt();
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
        addContentLabel("");
        addContentLabel("");
        addContentLabel("");
        addContentLabel("");
        body.add(dsp);

        pollingThread.start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                SocketClient.sendMessage("leave-game," + game.id + "," + currentUser);
                pollingThread.interrupt();
                setVisible(false);
                dispose();
                new MainWindow().setVisible(true);
            }
        });
    }

    public void addContentLabel(String text) {
        content.add(new DreamLabel(text));
    }

    public void createButton(String text, String iconPath, ActionListener listener, int width, int height) {
        try {
            DreamButton button = new DreamButton(text);
            button.setIcon(new ImageIcon(ImageUtils
                    .resize(ImageIO.read(SocketClient.class.getResource("Resources/" + iconPath)), width, height)));
            button.addActionListener(listener);
            content.add(button);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        LobbyWindow window = new LobbyWindow(Games.createNewGame());
        window.setVisible(true);
    }

}
