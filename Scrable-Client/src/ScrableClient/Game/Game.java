package ScrableClient.Game;

import ScrableServer.Words.Word;
import ScrableServer.Words.Words;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Game {
    public String id;
    public State gameState;

    public List<Player> players;

    public long startTime = -1;



    public Game(String id) {
        this.id = id;
        this.gameState = State.WAITING_FOR_PLAYERS;
        this.players = new ArrayList<>();
    }

    public void assignWords(){
        for (Player p : players){
            for (int i = 0; i < 26; i++){
                char c = (char) (65 + i);
                Word w = Words.getRandomWord(c);
                p.words.add(w);
            }
        }
    }
    public List<Word> getPlayerWords(String name){
        for (Player p : players){
            if (p.name.equals(name)){
                return p.words;
            }
        }
        return null;
    }

    public boolean hasStarted() {
        return startTime != -1 && System.currentTimeMillis() > startTime;
    }

    public boolean shouldStart() {
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


    public class Player {
        public String name;
        public boolean isReady = false;
        List<Word> words = new ArrayList<>();

        public Player(String name) {
            this.name = name;
        }

        public Word getWord(char c){
            for (Word w : words){
                if (w.name.toLowerCase().startsWith(String.valueOf(c).toLowerCase())){
                    return w;
                }
            }
            return null;
        }
        @Override
        public String toString() {
            Gson g = new GsonBuilder().create();

            return g.toJson(this);
        }
    }

    public enum State {
        WAITING_FOR_PLAYERS("Esperando jugadores"),
        IN_PROGRESS("En progreso"),
        FINISHED("Terminado");
        public String name;
        
        State(String name){
            this.name = name;
        }
    }

    @Override
    public String toString() {
        Gson g = new GsonBuilder().create();
        return g.toJson(this);
    }
}
