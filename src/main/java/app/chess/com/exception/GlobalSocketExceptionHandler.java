package app.chess.com.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalSocketExceptionHandler {

    private static final String ERROR_QUEUE = "/queue/errors";

    @MessageExceptionHandler(CustomErrorException.class)
    @SendToUser(ERROR_QUEUE)
    public String handleCustomException(CustomErrorException e) {
        return e.getMessage();
    }

    @MessageExceptionHandler(AuthenticationException.class)
    @SendToUser(ERROR_QUEUE)
    public String handleAuth(AuthenticationException e) {
        return e.getMessage();
    }

    @MessageExceptionHandler(BadCredentialsException.class)
    @SendToUser(ERROR_QUEUE)
    public String handleBadCredentials(BadCredentialsException e) {
        return "Wrong Credentials.";
    }

    @MessageExceptionHandler(DataIntegrityViolationException.class)
    @SendToUser(ERROR_QUEUE)
    public String handleConflict(DataIntegrityViolationException e) {
        return e.getMessage();
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(ERROR_QUEUE)
    public String handleValidation(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    @MessageExceptionHandler(RuntimeException.class)
    @SendToUser(ERROR_QUEUE)
    public String handleUnexpectedError(RuntimeException e) {
        return "An unexpected internal server error occurred.";
    }
}