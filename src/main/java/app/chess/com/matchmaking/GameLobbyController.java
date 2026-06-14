package app.chess.com.matchmaking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
public class GameLobbyController {

    @Autowired
    MatchmakingService matchmakingService;

    @MessageMapping("/matchmaking/join")
    public void addToLobby(Principal user) {
        log.info("User {} joined the matchmaking lobby", user.getName());
        matchmakingService.addToPool(user);
    }

    @MessageMapping("/matchmaking/cancel")
    public void removeFromLobby(Principal user) {
        log.info("User {} left the matchmaking lobby", user.getName());
        matchmakingService.removeFromPool(user);
    }

}
