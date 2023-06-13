package ScrableClient;

import ScrableClient.Interfaces.MainWindow;

public class Main {
    // TODO: Create singleton instead of this
    public static MainWindow mainWindow = new MainWindow();

    public static void main(String[] args) {
        mainWindow.setVisible(true);
    }
}
