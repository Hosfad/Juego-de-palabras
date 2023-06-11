package ScrableClient.GameListener;


import ScrableClient.MainClass;
import ScrableClient.SocketClient;
import ScrableServer.Game.Game;
import ScrableServer.ServerResponse;

public interface GameListener {

     void onGameTick(GameTick event);

      String getGameId();
      String getUserId();
      void onExit();

}
