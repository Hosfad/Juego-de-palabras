package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.Game.Game;
import ScrableClient.Main;
import ScrableServer.ServerUtils.Code;
import ScrableServer.Client;
import ScrableServer.Server;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class LobbyWindow extends DreamFrame {

    public String currentUser;
    private DreamPanel body, content;
    private Game currentGame;

    private DreamLabel connectedUsersNum = new DreamLabel("0");
    private List playerList = new List();

    public LobbyWindow(Game game, String currentUser) {
        super("Sala de espera", ImageUtils
                .resize((BufferedImage) ImageUtils.getImageFromUrl("https://mir-s3-cdn-cf.behance.net/projects/404/bbf0ae95440691.Y3JvcCwxMTUwLDkwMCwyMjUsMA.jpg"), 20, 20));
        this.currentGame = game;
        this.currentUser = currentUser;

        Client mainClient = MainWindow.instance.client;

        body = new DreamPanel();
        setSize(750, 600);
        setLocationRelativeTo(null);
        add(body, BorderLayout.CENTER);
        body.setBorder(new EmptyBorder(7, 8, 7, 8));

        body.add(content = new DreamPanel(), BorderLayout.NORTH);
        GridLayout grid = new GridLayout(0, 2);
        grid.setVgap(15);

        content.setLayout(grid);
        content.add(new DreamLabel("Id del juego: "));

        DreamTextField f = new DreamTextField();
        f.setText(game.id + "");
        f.setSize(100, 30);
        f.setEditable(false);

        JPanel idPanel = new JPanel();
        idPanel.setLayout(new FlowLayout());
        idPanel.add(f);

        DreamButton copyButton = new DreamButton("Copiar");
        idPanel.add(copyButton);

        copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(f.getText()), null));
        idPanel.setBackground(UIColours.BODY_COLOUR);

        content.add(idPanel);
        content.add(new DreamLabel("Estado: "));
        content.add(new DreamLabel(currentGame.gameState.name));
        content.add(new DreamLabel("Jugadores conectados: "));
        connectedUsersNum.setText(currentGame.players.size() + "");
        content.add(connectedUsersNum);

        createButton("Listo", "start.png", e -> mainClient.sendMessageToServer(Code.PLAYER_READY, currentUser));
        createButton("Salir de la sala", "stop.png", e -> onExit());

        playerList.setForeground(Color.white);
        playerList.setBackground(UIColours.BODY_COLOUR);

        DreamScrollPane dsp = new DreamScrollPane(playerList);
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        content.add(new DreamLabel(""));
        body.add(dsp);

        // Network Logic
        redraw();

        mainClient.addListener(Code.CONNECT, (args) -> {
            currentGame.addPlayer(args.data[0]);
            redraw();
        });

        mainClient.addListener(Code.DISCONNECT, (args) -> {
            currentGame.removePlayer(args.data[0]);
            redraw();
        });

        mainClient.addListener(Code.SHUTDOWN, (args) -> {
            setVisible(false);
            dispose();
            MainWindow.instance.setVisible(true);
            MainWindow.instance.disconnect();
        });

        mainClient.addListener(Code.PLAYER_READY, (args) -> {
            Game.Player player = currentGame.getPlayer(p -> p.name.equals(args.data[0]));
            player.isReady = !player.isReady;
            if(currentGame.shouldStart() && MainWindow.instance.server != null) {
                MainWindow.instance.server.sendMessageToClients(Code.START_GAME); 
            }
            redraw();
        });

        mainClient.addListener(Code.START_GAME, (args) -> {
            setVisible(false);
            dispose();
            GameWindow gameWindow = new GameWindow(currentGame, currentUser);
            gameWindow.setVisible(true);
        });

        // Server logic
        Server server = MainWindow.instance.server;

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

    public void redraw() {
        connectedUsersNum.setText(currentGame.players.size() + "");
        playerList.removeAll();

        for (Game.Player player : currentGame.players) {
            String ready = player.isReady ? "(Ready)" : "";
            playerList.add(player.name + ": " + ready);
        }
    }

    private void createButton(String text, String imageName, ActionListener action) {
        DreamButton button = new DreamButton(text);
        try {
            button.setIcon(new ImageIcon(
                    ImageUtils.resize(ImageIO.read(Main.class.getResource("Resources/" + imageName)), 20, 20)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        button.addActionListener(action);
        content.add(button);
    }

    public void onExit() {
        MainWindow win = MainWindow.instance;
        win.disconnect();
        win.setVisible(true);
        setVisible(false);
        dispose();
    }
}
