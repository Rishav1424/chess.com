package app.chess.com.game;

import app.chess.com.dto.ApiSuccessResponse;
import app.chess.com.dto.GameEntityResponse;
import app.chess.com.dto.GameStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/games/{gameId}")
public class GameRequestController {

    @Autowired
    GameService gameService;

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<GameEntityResponse>> getGameHistory(@PathVariable Long gameId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(gameService.getGame(gameId)));
    }

    @GetMapping("/live")
    public ResponseEntity<ApiSuccessResponse<GameStatusResponse>> getGameState(@PathVariable Long gameId) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(gameService.getGameStatus(gameId)));
    }
}
