package app.chess.com.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
public class GameSocketController {

    @Autowired
    GameService gameService;

    @MessageMapping("game/{gameId}/move")
    public void makeMove(@DestinationVariable Long gameId, String move, Principal principal) {
        log.info("Received move: {} from {} by {}", move, gameId, principal.getName());
        gameService.makeMove(gameId, move, principal.getName());
    }

    @MessageMapping("game/{gameId}/action/resign")
    public void resignGame(@DestinationVariable Long gameId, Principal principal) {
        log.info("Received resign request from {} for game {}", principal.getName(), gameId);
        gameService.handleResignation(gameId, principal.getName());

    }

    @MessageMapping("game/{gameId}/action/draw")
    public void drawRequest(@DestinationVariable Long gameId, Principal principal) {
        log.info("Received draw offer from {} for game {}", principal.getName(), gameId);
        gameService.handleDrawOffer(gameId, principal.getName());
    }
}
