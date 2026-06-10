package app.chess.com.game;

import app.chess.com.dto.ApiSuccessResponse;
import app.chess.com.dto.GameEntityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import app.chess.com.dto.GameStatusResponse;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/game/{gameId}")
public class GameRequestController {

    @Autowired
    GameService gameService;

    @GetMapping("/status")
    public ResponseEntity<ApiSuccessResponse<GameStatusResponse>> getGameState(@PathVariable Long gameId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(gameService.getGameStatus(gameId)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiSuccessResponse<GameEntityResponse>> getGameHistory(@PathVariable Long gameId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(gameService.getGame(gameId)));
    }
}
