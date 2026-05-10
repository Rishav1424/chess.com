package app.chess.com.matchmaking;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SerialMatchMaker<T> implements MatchMaker<T> {
    @Override
    public List<T> getMatch(List<T> pool) {
        if (pool.size() < 2) {
            return new ArrayList<>(); // Not enough players to make a match
        }

        // Return the first two elements
        return pool.subList(0, 2);
    }
}
