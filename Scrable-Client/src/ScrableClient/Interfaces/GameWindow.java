package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.Game.Game;
import ScrableServer.Client;
import ScrableServer.Server;
import ScrableServer.ServerUtils.Code;
import ScrableServer.Words.Word;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GameWindow extends DreamFrame {

	private DreamPanel body, content;
	public String currentUserId;
	public JLabel selectedLetter = new JLabel();
	public JLabel definition = new JLabel("Definicion : ");
	public Word selectedWord;
	private List playerList = new List();
	private Game currentGame;
	private int guesses = 0;
	long futureTimeMillis;

	Server server = MainWindow.instance.server;
	Client mainClient = MainWindow.instance.client;

	public JLabel remainingTime = new JLabel("Tiempo restante : 26:00");

	Thread timeThread = new Thread(() -> {
		long remainingTimeMillis;
		while ((remainingTimeMillis = futureTimeMillis - System.currentTimeMillis()) > 0) {
			long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMillis);
			long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingTimeMillis) % 60;

			String remain = String.format("%02d:%02d", remainingMinutes, remainingSeconds);
			remainingTime.setText("Tiempo restante : " + remain);
			try {
				Thread.sleep(600);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		endGame();
	});

	public void endGame() {
		if (server != null) {
			currentGame.players.sort(Comparator.comparingInt(p -> p.score));
			server.sendMessageToClients(Code.WINNER, currentGame.players.get(currentGame.players.size() - 1).name);
		} else {
			mainClient.sendMessageToServer(Code.WINNER, currentUserId);
		}
	}

	// Calculate the remaining time in milliseconds
	public GameWindow(Game game, String currentUserId) {
		super("Juego de palabras", ImageUtils.resize(
				(BufferedImage) Objects.requireNonNull(ImageUtils.getImageFromUrl("https://mir-s3-cdn-cf.behance.net/projects/404/bbf0ae95440691.Y3JvcCwxMTUwLDkwMCwyMjUsMA.jpg")),
				20, 20));
		futureTimeMillis = game.startTime + TimeUnit.MINUTES.toMillis(26);
		this.currentUserId = currentUserId;
		this.currentGame = game;

		for (Game.Player player : game.players) {
			player.isReady = false;
		}

		game.assignWords();

		definition.setForeground(Color.white);
		definition.setMaximumSize(new Dimension(300, 50));
		definition.setText("<html><body style='width: 250px'>" + "Definicion : " + game.getPlayer(p -> p.name.equals(currentUserId)).getWord('a').definition + "</body></html>");

		remainingTime.setForeground(Color.white);

		selectedWord = game.getPlayer(p -> p.name.equals(currentUserId)).getWord('A');
		body = new DreamPanel();
		setSize(750, 600);
		setLocationRelativeTo(null);
		add(body, BorderLayout.CENTER);
		body.setBorder(new EmptyBorder(7, 8, 7, 8));

		body.add(content = new DreamPanel(), BorderLayout.NORTH);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		DreamPanel buttonPanel = new DreamPanel();
		buttonPanel.setLayout(new GridLayout(5, 5));

		DreamButton[] buttons = new DreamButton[26];

		for (int i = 0; i < 26; i++) {
			buttons[i] = new DreamButton(String.valueOf((char) (65 + i))); // Letters A to Z
			buttons[i].addActionListener(e -> {
				char selectedChar = ((DreamButton) e.getSource()).getText().charAt(0);
				selectedLetter.setText("Letra selecionada : " + selectedChar);
				selectedWord = game.getPlayer(p -> p.name.equals(currentUserId)).getWord(selectedChar);
				definition.setText("<html><body style='width: 250px'>" + "Definicion : " + selectedWord.definition + "</body></html>");
			});
			buttonPanel.add(buttons[i]);
		}
		content.add(buttonPanel);

		content.add(new DreamLabel("⠀"));
		content.add(remainingTime);
		content.add(new DreamLabel("⠀"));
		content.add(new DreamLabel("Elige la palabra correspondiente"));
		content.add(new DreamLabel("⠀"));
		content.add(definition);

		DreamPanel currentWordPanel = new DreamPanel();
		currentWordPanel.setBackground(UIColours.BODY_COLOUR);
		currentWordPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

		selectedLetter.setText("Palabra actual: A");
		selectedLetter.setForeground(Color.white);
		currentWordPanel.add(selectedLetter);

		DreamHintTextField userSelectedWord = new DreamHintTextField("");
		userSelectedWord.setPreferredSize(new Dimension(200, 30)); // Increase the width to 200

		currentWordPanel.add(userSelectedWord);

		content.add(currentWordPanel);
		content.add(new DreamLabel("⠀"));
		content.add(new DreamLabel("⠀"));

		DreamButton submitWord = new DreamButton("Enviar palabra");
		submitWord.addActionListener(e -> {
			if (userSelectedWord.getText().isEmpty()) return;
			guesses++;
			String word = selectedWord.name;
			String selectedWord = userSelectedWord.getText();
			userSelectedWord.setText("");
			for (int i = 0; i < buttons.length; i++) {
				DreamButton dreamButton = buttons[i];
				if (dreamButton.getText().equalsIgnoreCase(word.charAt(0) + "")) {
					dreamButton.setEnabled(false);
					dreamButton.setText("");
					if (guesses < buttons.length) {
						int j = (i + 1) % buttons.length;
						while (!buttons[j].isEnabled())
							j = (j + 1) % buttons.length;
						buttons[j].doClick();
						mainClient.sendMessageToServer(Code.WORD_GUESS, currentUserId, word, selectedWord);
					} else {
						JOptionPane.showMessageDialog(null, "¡El juego ha terminado espera por tus rivales!");
						endGame();
					}
					break;
				}
			}
		});
		userSelectedWord.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER){
					submitWord.doClick();
				}
			}
		});

		content.add(submitWord);
		content.add(new DreamLabel("⠀"));
		content.add(new DreamLabel("⠀"));
		content.add(new DreamLabel("⠀"));

		playerList.setForeground(Color.white);
		playerList.setBackground(UIColours.BODY_COLOUR);

		DreamScrollPane dsp = new DreamScrollPane(playerList);
		content.add(dsp, BorderLayout.SOUTH);
		timeThread.start();

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				MainWindow.instance.disconnect();
			}
		});

		redraw();

		// Network Logic
		mainClient.addListener(Code.WORD_GUESS, args -> {
			Game.Player player = game.getPlayer(p -> p.name.equals(args.data[0]));
			player.score += Boolean.parseBoolean(args.data[1]) ? 1 : 0;
			redraw();
		});

		mainClient.addListener(Code.WINNER, args -> {
			StringBuilder sb = new StringBuilder();
			for (Game.Player player : currentGame.players) {
				sb.append(player.name).append(": ").append(player.score).append("\n");
			}
			String scoreboard = sb.toString();
			if (args.data[0].equals(currentUserId)) {
				JOptionPane.showMessageDialog(null, "Ganaste!\n\n" + scoreboard);
			} else {
				JOptionPane.showMessageDialog(null, currentUserId + " es el ganador!\n\n" + scoreboard);
			}
			setVisible(false);
			dispose();
			MainWindow.instance.disconnect();
			MainWindow.instance.setVisible(true);
		});

		if (server == null)
			return;

		server.addListener(Code.WINNER, args -> {
			currentGame.getPlayer(p -> p.name.equals(args.data[0])).isReady = true;
			if (currentGame.shouldStart())
				endGame();
		});
		server.addListener(Code.WORD_GUESS, args -> {
			String userId = args.data[0];
			String actualWord = args.data[1];
			String userWord = args.data[2];
			server.sendMessageToClients(Code.WORD_GUESS, userId, actualWord.equalsIgnoreCase(userWord) + "");
		});
	}

	public void redraw() {
		playerList.removeAll();

		for (Game.Player player : currentGame.players) {
			playerList.add(player.name + ": " + player.score);
		}
	}

}
