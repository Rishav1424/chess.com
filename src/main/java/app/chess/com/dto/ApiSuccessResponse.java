package app.chess.com.dto;

public record ApiSuccessResponse<T>(boolean success, T data, String message) {

    public ApiSuccessResponse(T data){
        this(data, null);
    }

    public ApiSuccessResponse(T data, String message) {
        this(true, data, message);
    }
}