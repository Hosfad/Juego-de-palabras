package ScrableServer.Game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Game {
    public long id;
    public State gameState;

    public List<Player> players;

    public long startTime = -1;

    public String currentlyPlaying ;




    public Game(long id) {
        this.id = id;
        this.gameState = State.WAITING_FOR_PLAYERS;
        this.players = new ArrayList<>();
    }

    public boolean hasStarted(){
        return startTime != -1 && System.currentTimeMillis() > startTime;
    }
    public boolean shouldStart(){
        return players.size() >= 2 && players.stream().allMatch(i -> i.isReady);
    }

    public void setGameState(State gameState) {
        this.gameState = gameState;
    }

    public boolean addPlayer(String name) {
        if (hasPlayer(name)) {
            return false;
        }
        Player p = new Player(name);
        players.add(p);
        return true;
    }

    public boolean removePlayer(String name) {
        if (!hasPlayer(name)) {
            return true;
        }
        players.removeIf(i -> i.name.equals(name));
        return true;
    }

    public boolean hasPlayer(String name) {
        for (Player player : players) {
            if (player.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Player getPlayer(Predicate<Player> filter) {
        for (Player player : players) {
            if (filter.test(player)) {
                return player;
            }
        }
        return null;
    }
    public Player getCurrenRoundPlayer(){
        return getPlayer(i -> i.name.equals(currentlyPlaying));
    }


    public class Player {
        public String name;
        public int score = 0;
        public boolean isReady = false;
        @Expose(serialize = false,deserialize = false)
        public long lastPing;
        public Player(String name) {
            this.name = name;
            this.score = 0;
            this.lastPing = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            Gson g = new GsonBuilder().create();

            return g.toJson(this);
        }
    }

    public enum State {
        WAITING_FOR_PLAYERS,
        IN_PROGRESS,
        FINISHED
    }

    @Override
    public String toString() {
        Gson g = new GsonBuilder().create();
        return g.toJson(this);
    }
}
