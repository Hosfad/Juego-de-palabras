package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.GameListener.GameListener;
import ScrableClient.GameListener.GameTick;
import ScrableClient.MainClass;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.Game.Games;
import ScrableServer.ServerResponse;
import com.google.gson.Gson;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class LobbyWindow extends DreamFrame implements GameListener {

    private DreamPanel body, content;
    Game currentGame;
    public String currentUser;

    DreamLabel connectedUsersNum = new DreamLabel("0");
    List playerList = new List();

    public LobbyWindow(Game game , String currentUser) {

        super("Sala de espera",ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"),20,20));
         body = new DreamPanel();
         this.currentGame = game;
         this.currentUser = currentUser;
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
        f.setSize(100,30);
        f.setEditable(false);
        JPanel idPanel = new JPanel();
        idPanel.setLayout(new FlowLayout());
        idPanel.add(f);
        DreamButton copyButton = new DreamButton("Copy");
        idPanel.add(copyButton);
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(f.getText()), null);
            }
        });

        idPanel.setBackground(UIColours.BODY_COLOUR);


        content.add(idPanel);
        content.add(new DreamLabel("Status: ") );
        content.add(new DreamLabel( currentGame.gameState.name()));
        content.add(new DreamLabel("Connected users: ") );
        connectedUsersNum.setText(currentGame.players.size() + "");
        content.add(connectedUsersNum);
        DreamButton readyButton = new DreamButton("Ready");
        try {
            readyButton.setIcon(new ImageIcon(ImageUtils.resize(ImageIO.read(SocketClient.class.getResource("Resources/start.png")) ,20,20)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        readyButton.addActionListener(e->{
            String res = SocketClient.sendMessage("player-ready," + game.id + "," + currentUser);
            if (res.startsWith(ServerResponse.OK.getName())) {
                readyButton.setEnabled(false);
            }

        });
        content.add(readyButton);
        DreamButton leaveGame = new DreamButton("Leave Game");
        try {
            leaveGame.setIcon(new ImageIcon(ImageUtils.resize( ImageIO.read(SocketClient.class.getResource("Resources/stop.png")) ,20,20 )));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        leaveGame.addActionListener(e->{
            onExit();
        });
        content.add(leaveGame);
        playerList.setForeground(Color.white);
        playerList.setBackground(UIColours.BODY_COLOUR);
        DreamScrollPane dsp = new DreamScrollPane(playerList);
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        body.add(dsp);

        MainClass.addListener(this);

    }



    @Override
    public void onGameTick(GameTick event) {
        // Handle ui updates here
        connectedUsersNum.setText(event.getGame().players.size() + "");
        playerList.removeAll();
        for (Game.Player player : event.getGame().players) {
            String ready = player.isReady ? "(Ready)" : "";
            playerList.add("Player : " + player.name + " " + ready);
        }


        if (event.status.equals(ServerResponse.GAME_NOT_FOUND)){
            this.setVisible(false);
            this.dispose();
            MainClass.mainWindow.setVisible(true);
            return;
        }

        if (event.status.equals(ServerResponse.OK) ) {

            if (event.getGame().gameState.equals(Game.State.IN_PROGRESS)){
                this.setVisible(false);
                this.dispose();
                GameWindow gameWindow = new GameWindow(event.getGame(), currentUser);
                gameWindow.setVisible(true);
                return;
            }

        }

    }

    @Override
    public String getGameId() {
        return currentGame.id + "";
    }

    @Override
    public String getUserId() {
        return currentUser;
    }

    @Override
    public void onExit() {
        // Handle exit here
        String res = SocketClient.sendMessage("leave-game," + getGameId() + "," + getUserId());
        this.setVisible(false);
        this.dispose();
        MainClass.mainWindow.setVisible(true);
        MainClass.removeListener(MainClass.getInstance(this.getClass()));

    }
}
