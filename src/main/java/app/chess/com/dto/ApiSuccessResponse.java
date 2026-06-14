package app.chess.com.dto;

public record ApiSuccessResponse<T>(boolean success, T data) {
    public ApiSuccessResponse(T data) {
        this(true, data);
    }
}