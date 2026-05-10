package app.chess.com.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/api/game/{gameId}")
public class RequestController {

    @Autowired
    GameService gameService;

    @GetMapping("/status")
    public ResponseEntity<GameState> getGameState(@PathVariable Long gameId) {
        GameState state = gameService.getGameState(gameId);
        if(state == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(state);
    }

    @GetMapping("/history")
    public ResponseEntity<GameEntity> getGameHistory(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(gameService.getGame(gameId));
        } catch (NoSuchElementException e) {
            log.error("Game not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
