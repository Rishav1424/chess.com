package app.chess.com.matchmaking;

import app.chess.com.game.GameService;
import app.chess.com.matchmaking.dto.MatchFoundNotification;
import com.github.bhlangonijr.chesslib.Side;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.security.Principal;
import java.sql.Time;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class MatchmakingService {

    @Autowired
    GameService gameService;

    @Autowired
    private MatchMaker<String> matchMaker;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    private static final String POOL_KEY = "MatchmakingPool";

    public void addToPool(Principal user) {
        redisTemplate.opsForZSet().add(POOL_KEY, user.getName(), Instant.now().getEpochSecond());
    }

    public void removeFromPool(Principal user) {
        redisTemplate.opsForZSet().remove(POOL_KEY, user.getName());
    }

    public List<String> extractPlayers() {
        List<String> players = redisTemplate.opsForZSet().randomMembers(POOL_KEY, 2);
        List<String> match = matchMaker.getMatch(players);

        for (String player : match) {
            redisTemplate.opsForZSet().remove(POOL_KEY, player);
        }

        return match;
    }

    @Scheduled(fixedRate = 10000)
    public void matchPlayer() {
        Long poolSize = redisTemplate.opsForZSet().zCard(POOL_KEY);
        while((poolSize-=2) >= 0){
            List<String> players = extractPlayers();
            if (players.isEmpty()) return;

            String whitePlayer = players.get(0);
            String blackPlayer = players.get(1);

            Long gameId = gameService.createGame(whitePlayer, blackPlayer);

            MatchFoundNotification whiteNotification = new MatchFoundNotification(gameId, blackPlayer, Side.WHITE);
            MatchFoundNotification blackNotification = new MatchFoundNotification(gameId, whitePlayer, Side.BLACK);

            log.info("whiteNotification: {}", whiteNotification);
            log.info("blackNotification: {}", blackNotification);

            simpMessagingTemplate.convertAndSendToUser(whitePlayer, "/queue/match-making", whiteNotification);
            simpMessagingTemplate.convertAndSendToUser(blackPlayer, "/queue/match-making", blackNotification);
        }
    }

}
