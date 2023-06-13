package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.GameListener.GameListener;
import ScrableClient.GameListener.GameTick;
import ScrableClient.Main;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.ServerUtils.Code;
import ScrableServer.Client;
import ScrableServer.Server;
import ScrableServer.ServerResponse;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class LobbyWindow extends DreamFrame implements GameListener {

    public String currentUser;
    private DreamPanel body, content;
    private Game currentGame;

    private DreamLabel connectedUsersNum = new DreamLabel("0");
    private List playerList = new List();

    public LobbyWindow(Game game, String currentUser) {
        super("Sala de espera", ImageUtils.resize((BufferedImage) ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png"), 20, 20));
        this.currentGame = game;
        this.currentUser = currentUser;

        Client mainClient = Main.mainWindow.client;

        body = new DreamPanel();
        setSize(500, 600);
        setLocationRelativeTo(null);
        add(body, BorderLayout.CENTER);
        body.setBorder(new EmptyBorder(7, 8, 7, 8));

        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        GridLayout grid = new GridLayout(0, 2);
        grid.setVgap(15);

        content.setLayout(grid);
        content.add(new DreamLabel("Game id: "));

        DreamTextField f = new DreamTextField();
        f.setText(game.id + "");
        f.setSize(100, 30);
        f.setEditable(false);

        JPanel idPanel = new JPanel();
        idPanel.setLayout(new FlowLayout());
        idPanel.add(f);

        DreamButton copyButton = new DreamButton("Copy");
        idPanel.add(copyButton);

        copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(f.getText()), null));
        idPanel.setBackground(UIColours.BODY_COLOUR);

        content.add(idPanel);
        content.add(new DreamLabel("Status: "));
        content.add(new DreamLabel(currentGame.gameState.name()));
        content.add(new DreamLabel("Connected users: "));
        connectedUsersNum.setText(currentGame.players.size() + "");
        content.add(connectedUsersNum);

        DreamButton readyButton = new DreamButton("Ready");
        try {
            readyButton.setIcon(new ImageIcon(
                    ImageUtils.resize(ImageIO.read(SocketClient.class.getResource("Resources/start.png")), 20, 20)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        readyButton.addActionListener(e -> mainClient.sendMessageToServer(Code.PLAYER_READY, currentUser));
        content.add(readyButton);

        DreamButton leaveGame = new DreamButton("Leave Game");
        try {
            leaveGame.setIcon(new ImageIcon(
                    ImageUtils.resize(ImageIO.read(SocketClient.class.getResource("Resources/stop.png")), 20, 20)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        leaveGame.addActionListener(e -> onExit());

        content.add(leaveGame);
        playerList.setForeground(Color.white);
        playerList.setBackground(UIColours.BODY_COLOUR);
        DreamScrollPane dsp = new DreamScrollPane(playerList);
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        body.add(dsp);

        // Network Logic
        update();

        mainClient.addListener(Code.CONNECT, (args) -> {
            currentGame.addPlayer(args.data[0]);
            update();
        });

        mainClient.addListener(Code.DISCONNECT, (args) -> {
            currentGame.removePlayer(args.data[0]);
            update();
        });

        mainClient.addListener(Code.SHUTDOWN, (args) -> {
            setVisible(false);
            dispose();
            Main.mainWindow.setVisible(true);
            Main.mainWindow.disconnect(); 
        });

        mainClient.addListener(Code.PLAYER_READY, (args) -> {
            Game.Player player = currentGame.getPlayer(p -> p.name.equals(args.data[0]));
            player.isReady = !player.isReady;
            update();
        });

        // Server logic
        Server server = Main.mainWindow.server;

        if (server == null)
            return;

        server.addListener(Code.JOIN, (args) -> {
            StringBuilder builder = new StringBuilder();
            for (Game.Player player : currentGame.players) {
                builder.append(player.name + " " + player.isReady + " ");
            }
            server.sendMessageToClients(Code.JOIN, builder.toString());
        });

        server.addListener(Code.PLAYER_READY, args -> {
            server.sendMessageToClients(Code.PLAYER_READY, args.message);
        });
    }

    public void update() {
        connectedUsersNum.setText(currentGame.players.size() + "");
        playerList.removeAll();

        for (Game.Player player : currentGame.players) {
            String ready = player.isReady ? "(Ready)" : "";
            playerList.add(player.name + ": " + ready);
        }
    }

    @Override
    public void onGameTick(GameTick event) {
        if (event.status.equals(ServerResponse.OK)) {
            if (event.getGame().gameState.equals(Game.State.IN_PROGRESS)) {
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
        MainWindow win = Main.mainWindow;
        win.disconnect();
        win.setVisible(true);
        setVisible(false);
        dispose();
    }
}
