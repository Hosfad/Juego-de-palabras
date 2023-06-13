package ScrableClient.GameListener;

public interface GameListener {

      void onGameTick(GameTick event);

      String getGameId();

      String getUserId();

      void onExit();

}
