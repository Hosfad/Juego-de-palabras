package ScrableClient.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Games {
    private static List<Game> activeGames = new ArrayList<>();

    static {
        cleanUp();
    }


    public static Game createNewGame() {
        Game g = new Game(System.currentTimeMillis());
        activeGames.add(g);
        return g;
    }

    private static void cleanUp() {
        if (activeGames == null) return;
        activeGames.removeIf(i -> i.gameState == Game.State.FINISHED || i.players.isEmpty());
    }

    public static boolean hasGame(Predicate<Game> filter) {
        for (Game game : activeGames) {
            if (filter.test(game)) {
                return true;
            }
        }
        return false;
    }

    public static boolean removeGame(Predicate<Game> filter) {
        for (Game game : activeGames) {
            if (filter.test(game)) {
                activeGames.remove(game);
                return true;
            }
        }
        return false;
    }

    public static List<Game> all(Predicate<Game> g) {
        List<Game> games = new ArrayList<>();
        for (Game game : activeGames) {
            if (g.test(game)) {
                games.add(game);
            }
        }
        return games;
    }
    public static List<Game> all() {
        return activeGames;
    }
    public static Game getGame(Predicate<Game> filter) {
        for (Game game : activeGames) {
            if (filter.test(game)) {
                return game;
            }
        }
        return null;
    }
}
