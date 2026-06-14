package app.chess.com.dto;

import java.time.Duration;

public record GameStatusResponse(String fen, String whitePlayerName, String blackPlayerName, Duration whiteTime,
                                 Duration blackTime, String[] moves, String pendingDrawOffer) {
}
