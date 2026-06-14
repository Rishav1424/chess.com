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

    @MessageMapping("games/{gameId}/move")
    public void makeMove(@DestinationVariable Long gameId, String move, Principal principal) {
        gameService.makeMove(gameId, move, principal.getName());
    }

    @MessageMapping("games/{gameId}/resign")
    public void resignGame(@DestinationVariable Long gameId, Principal principal) {
        gameService.resign(gameId, principal.getName());
    }

    @MessageMapping("games/{gameId}/draw/offer")
    public void offerDraw(@DestinationVariable Long gameId, Principal principal) {
        gameService.offerDraw(gameId, principal.getName());
    }

    @MessageMapping("games/{gameId}/draw/accept")
    public void acceptDraw(@DestinationVariable Long gameId, Principal principal) {
        gameService.acceptDraw(gameId, principal.getName());
    }

    @MessageMapping("games/{gameId}/draw/decline")
    public void declineDraw(@DestinationVariable Long gameId, Principal principal) {
        gameService.declineDraw(gameId, principal.getName());
    }
}
