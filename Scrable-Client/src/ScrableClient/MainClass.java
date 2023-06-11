package ScrableClient;

import ScrableClient.GameListener.GameListener;
import ScrableClient.GameListener.GameTick;
import ScrableClient.Interfaces.GameWindow;
import ScrableClient.Interfaces.LobbyWindow;
import ScrableClient.Interfaces.MainWindow;
import ScrableServer.Game.Game;

import java.util.ArrayList;
import java.util.List;

public class MainClass {
    public static List<GameListener> listeners = new ArrayList<>();

    public static MainWindow mainWindow = new MainWindow();
    public static void main(String[] args) {


        Thread socketClientThread = new Thread(() -> {

            while (true){
                try {

                        for (GameListener listener : listeners) {
                            // Trigger on tick for all listeners
                            String res= SocketClient.sendMessage("get-game-info," + listener.getGameId() + "," + listener.getUserId());
                            GameTick event = new GameTick(res);
                            listener.onGameTick(event);
                        }

                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        });

        mainWindow.setVisible(true);
        socketClientThread.start();
    }
    public static void addListener(GameListener listener){
        listeners.add(listener);
    }
    public static void removeListener(GameListener listener){
        listeners.remove(listener);
    }
    public static void removeAllListeners(){
        listeners.clear();
    }
    public static GameListener getInstance(Class clazz){
        for (GameListener l : listeners) {
            if (l.getClass() == clazz){
                return l;
            }
        }
        return null;
    }
    public static boolean hasListener(Class clazz){
        for (GameListener l : listeners) {
            if (l.getClass() == clazz){
                return true;
            }
        }
        return false;
    }

}
