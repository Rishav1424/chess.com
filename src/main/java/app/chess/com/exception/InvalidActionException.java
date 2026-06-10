package app.chess.com.exception;

import org.springframework.http.HttpStatus;

public class InvalidActionException extends CustomErrorException {

    public InvalidActionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus statusCode() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String errorCode() {
        return "INVALID_ACTION";
    }
}
