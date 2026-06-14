package app.chess.com.dto;

import java.util.List;

public record PageResponse<T>(List<T> items, PageInfo page) {
    public record PageInfo(int limit, int offset, long total) {}
}