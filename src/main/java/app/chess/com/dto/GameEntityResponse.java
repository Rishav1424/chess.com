package app.chess.com.dto;

import app.chess.com.game.GameEntity;
import app.chess.com.game.GameStatus;

import java.time.Instant;

public record GameEntityResponse(Long id, String whitePlayerName, String blackPlayerName, Instant started,
                                 Instant finished, GameStatus status, String[] moves) {
    public GameEntityResponse(GameEntity entity) {
        this(entity.getId(), entity.getWhitePlayer().getUsername(), entity.getBlackPlayer().getUsername(), entity.getTimestamp(), entity.getUpdated(), entity.getStatus(), entity.getMoves());
    }
}
