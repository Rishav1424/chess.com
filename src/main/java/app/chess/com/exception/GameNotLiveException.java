package app.chess.com.exception;

import org.springframework.http.HttpStatus;

public class GameNotLiveException extends CustomErrorException {

    public GameNotLiveException(Long gameId) {
        super("Game " + gameId + " has ended. Fetch GET /games/" + gameId + " for the final record.");
    }

    @Override
    public HttpStatus statusCode() {
        return HttpStatus.GONE; // 410
    }

    @Override
    public String errorCode() {
        return "GAME_NOT_LIVE";
    }
}