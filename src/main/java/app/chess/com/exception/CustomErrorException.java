package app.chess.com.exception;

import org.springframework.http.HttpStatus;

public abstract class CustomErrorException extends RuntimeException {
    public CustomErrorException(String message) {
        super(message);
    }

    public abstract HttpStatus statusCode();
    public abstract String errorCode();
}
