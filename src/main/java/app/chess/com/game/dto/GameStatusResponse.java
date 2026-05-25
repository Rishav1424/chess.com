package app.chess.com.game.dto;

import java.time.Duration;

public record GameStatusResponse(String fen, String whitePlayer, String blackPlayer, Duration whiteTime, Duration blackTime, String[] moves) {
}
