package app.chess.com.game;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, Long> {
    Page<GameEntity> findByWhitePlayer_UsernameOrBlackPlayer_Username(String white, String black, Pageable page);
    Long countByWhitePlayer_UsernameAndStatusIn(String username, List<GameStatus> status);
    Long countByBlackPlayer_UsernameAndStatusIn(String username, List<GameStatus> status);

}
