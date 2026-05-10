package app.chess.com.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@Controller
public class GameController {

    @Autowired
    GameService gameService;

    @MessageMapping("game/{gameId}/move")
    public void makeMove(@DestinationVariable Long gameId, String move, Principal principal) {
        log.info("Received move: {} from {} by {}", move, gameId, principal.getName());
        try {
            gameService.makeMove(gameId, move, principal.getName());
        } catch (IllegalAccessException e) {
            log.error("Error processing move: {} by {}", e.getMessage(), principal.getName());
        }
    }

    @MessageMapping("game/{gameId}/action")
    public void resignGame(@DestinationVariable Long gameId, @Payload String action, Principal principal) {
        try {
            if (action.equals("RESIGN")) {
                log.info("Received resign request from {} for game {}", principal.getName(), gameId);
                gameService.handleResignation(gameId, principal.getName());
            } else if (action.equals("DRAW")) {
                log.info("Received draw offer from {} for game {}", principal.getName(), gameId);
                gameService.handleDrawOffer(gameId, principal.getName());
            }
        } catch (IllegalAccessException e) {
            log.error("Error processing action: {} by {}", e.getMessage(), principal.getName());

        }
    }
}
