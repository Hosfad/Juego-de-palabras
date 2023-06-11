package ScrableServer;

public enum ServerResponse {
    SERVER_ERROR("SERVER ERROR"),
    OK("OK"),
    NAME_TAKEN("NAME TAKEN"),
    GAME_ALREADY_STARTED("GAME ALREADY STARTED"),
    GAME_ENDED("GAME ENDED"),
    GAME_NOT_FOUND("404 not found");

    private final String name;

    public String getName(){
        return name;
    }

     ServerResponse(String name){
        this.name = name;
    }

}
