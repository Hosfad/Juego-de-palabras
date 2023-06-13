package ScrableClient.GameListener;

import ScrableServer.Game.Game;
import ScrableServer.ServerResponse;
import com.google.gson.Gson;

public class GameTick {
    public ServerResponse status;
    public String message;

    public GameTick(String res) {
        String[] split = res.split(":");
        status = ServerResponse.valueOf(split[0]);
        message = res.replace(split[0] + ":", "");
    }

    public Game getGame() {
        Gson gson = new Gson();
        return gson.fromJson(message, Game.class);
    }
}
