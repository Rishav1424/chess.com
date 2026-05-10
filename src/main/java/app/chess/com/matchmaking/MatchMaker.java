package app.chess.com.matchmaking;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface MatchMaker<T> {
    @Bean
    List<T> getMatch(List<T> pool);
}
