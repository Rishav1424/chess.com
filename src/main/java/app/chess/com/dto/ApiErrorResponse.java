package app.chess.com.dto;

public record ApiErrorResponse(boolean success, String error, String message) {
    public ApiErrorResponse(String error, String message){
        this(false, error, message);
    }
}
