package ScrableClient.Interfaces;

import ScrableClient.DreamUI.UIColours;
import ScrableClient.DreamUI.components.*;
import ScrableClient.DreamUI.utils.ImageUtils;
import ScrableClient.Game.Game;
import ScrableServer.Client;
import ScrableServer.Server;
import ScrableServer.ServerUtils;
import ScrableServer.Words.Word;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GameWindow extends DreamFrame {

	private DreamPanel body, content;
	public String currentUserId;
	public JLabel selectedLetter = new JLabel();
	public JLabel difinition = new JLabel("Difinicion : ");
	public Word selectedWord;
	private List playerList = new List();
	long futureTimeMillis;

	public JLabel remainingTime = new JLabel("Tiempo restante : 26:00");

	Thread timeThread = new Thread(() -> {
		while (true) {

			long remainingTimeMillis = futureTimeMillis - System.currentTimeMillis();
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
	});

	// Calculate the remaining time in milliseconds
	public GameWindow(Game game, String currentUserId) {
		super("Juego de palabras", ImageUtils
				.resize((BufferedImage) Objects
						.requireNonNull(ImageUtils.getImageFromUrl("https://i.imgur.com/Ir30QMW.png")), 20, 20));
		futureTimeMillis = game.startTime + TimeUnit.MINUTES.toMillis(26);

		System.out.println("Construct " + game.startTime);
		game.assignWords();
		difinition.setForeground(Color.white);
		difinition.setMaximumSize(new Dimension(300, 50));
		difinition.setText("<html><body style='width: 200px'>" + "Difinicion : " + game.getPlayer(p -> p.name.equals(currentUserId)).getWord('a').definition + "</body></html>");

		remainingTime.setForeground(Color.white);

		selectedWord = game.getPlayer(p -> p.name.equals(currentUserId)).getWord('A');
		body = new DreamPanel();
		this.currentUserId = currentUserId;
		setSize(500, 600);
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
				difinition.setText("<html><body style='width: 200px'>" + "Difinicion : " + selectedWord.definition + "</body></html>");


			});
			buttonPanel.add(buttons[i]);
		}
		content.add(buttonPanel);

		content.add(new DreamLabel("⠀"));
		content.add(remainingTime);
		content.add(new DreamLabel("⠀"));
		content.add(new DreamLabel("Elige la palabra correspondiente"));
		content.add(new DreamLabel("⠀"));
		content.add(difinition);

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
			// Enviar respuesta al servedor
			String word = selectedWord.name;
			String selectedWord = userSelectedWord.getText();
			MainWindow.instance.client.sendMessageToServer(ServerUtils.Code.WORD_GUESS, currentUserId, word,
					selectedWord);
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
	}

	public static void main(String[] args) {
		new GameWindow(new Game("asd"), "").setVisible(true);
	}


	public void onExit() {
		MainWindow win = MainWindow.instance;
		win.disconnect();
		win.setVisible(true);
		setVisible(false);
		dispose();
	}
}
