package app.chess.com.exception;

import app.chess.com.dto.ApiErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler(CustomErrorException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomException(CustomErrorException e) {
        return ResponseEntity.status(e.statusCode()).body(new ApiErrorResponse(e.errorCode(), e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErrorResponse("AUTHENTICATION_ERROR", e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErrorResponse("AUTHENTICATION_ERROR", "Wrong Credentials."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse("CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ApiErrorResponse.FieldError> details = e.getBindingResult().getFieldErrors().stream().map(f -> new ApiErrorResponse.FieldError(f.getField(), f.getDefaultMessage())).toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed.", details));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(RuntimeException e) {
        return ResponseEntity.internalServerError().body(new ApiErrorResponse("INTERNAL_ERROR", "An unexpected error occurred."));
    }

}