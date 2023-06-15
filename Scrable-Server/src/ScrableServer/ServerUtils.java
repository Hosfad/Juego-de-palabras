package ScrableServer;

public class ServerUtils {
    public enum Code {
        DISCONNECT,
        CONNECT,
        MESSAGE,
        SHUTDOWN,
        DATA,
        PLAYER_READY,
        JOIN,
        START_GAME,
        WORD_GUESS
    }

    public static class Args {
        public Code code;
        public String message;
        public String[] data;

        public Args(Code code, String message) {
            this.code = code;
            this.message = message;
            this.data = message.split(" ");
        }

        @Override
        public String toString() {
            return message;
        }
    }

    public static Args parseMessage(String args) {
        String[] values = args.split(" ");
        Code code = Code.values()[Integer.parseInt(values[0])];
        args = args.substring(values[0].length() + 1);
        return new Args(code, args);
    }
}