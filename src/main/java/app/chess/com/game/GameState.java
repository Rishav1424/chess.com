package app.chess.com.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameState implements Serializable {
    private String fen;
    private String whitePlayer;
    private String blackPlayer;
    private Duration whiteTime;
    private Duration blackTime;
    private Instant lastMoveTime;

    public GameState(String whitePlayer, String blackPlayer) {
        this.fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.whiteTime = GameService.TOTAL_TIME;
        this.blackTime = GameService.TOTAL_TIME;
        this.lastMoveTime = Instant.now();

    }
}
