package app.chess.com.exception;

import org.springframework.http.HttpStatus;

public class GameNotFoundException extends CustomErrorException {

    public GameNotFoundException(Long gameId) {
        super("Game " + gameId + " does not exist.");
    }

    @Override
    public HttpStatus statusCode() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String errorCode() {
        return "GAME_NOT_FOUND";
    }
}