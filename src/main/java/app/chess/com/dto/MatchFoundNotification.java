package app.chess.com.dto;

import com.github.bhlangonijr.chesslib.Side;

public record MatchFoundNotification(Long gameId, String opponentId, Side playerSide) {
}
