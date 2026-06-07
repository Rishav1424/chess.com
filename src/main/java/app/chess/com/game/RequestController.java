package app.chess.com.game;

import app.chess.com.game.dto.GameEntityResponse;
import app.chess.com.game.dto.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import app.chess.com.game.dto.GameStatusResponse;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/api/game/{gameId}")
public class RequestController {

    @Autowired
    GameService gameService;

    @GetMapping("/status")
    public ResponseEntity<Object> getGameState(@PathVariable Long gameId) {
        try {
            GameStatusResponse state = gameService.getGameStatus(gameId);
            return ResponseEntity.ok(state);
        }catch (IllegalAccessException e){
            return ResponseEntity.status(401).body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Object> getGameHistory(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(gameService.getGame(gameId));
        } catch (NoSuchElementException e) {
            log.error("Game not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
