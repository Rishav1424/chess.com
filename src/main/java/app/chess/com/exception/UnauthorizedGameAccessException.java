package app.chess.com.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedGameAccessException extends CustomErrorException {
    public UnauthorizedGameAccessException(){
        super("You are not authorized to access this game");
    }

    @Override
    public HttpStatus statusCode() {
        return HttpStatus.FORBIDDEN;
    }

    @Override
    public String errorCode() {
        return "AUTHORIZATION_ERROR";
    }
}
