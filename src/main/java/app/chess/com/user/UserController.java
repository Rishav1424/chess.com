package app.chess.com.user;

import app.chess.com.dto.ApiSuccessResponse;
import app.chess.com.game.GameEntity;
import app.chess.com.game.GameRepository;
import app.chess.com.game.GameStatus;
import app.chess.com.dto.GameEntityResponse;
import app.chess.com.dto.UserStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    GameRepository gameRepository;

    @GetMapping("/me/stats")
    public ResponseEntity<ApiSuccessResponse<UserStats>> getUserStats(Principal user){

        List<GameStatus> whiteWinStatuses = List.of(GameStatus.WON_WHITE_CHECKMATE, GameStatus.WON_WHITE_RESIGNATION, GameStatus.WON_WHITE_TIMEOUT);
        List<GameStatus> blackWinStatuses = List.of(GameStatus.WON_BLACK_CHECKMATE, GameStatus.WON_BLACK_RESIGNATION, GameStatus.WON_BLACK_TIMEOUT);
        List<GameStatus> drawStatuses = List.of(GameStatus.DRAW_STALEMATE, GameStatus.DRAW_AGREEMENT, GameStatus.DRAW_FIFTY_MOVE_RULE, GameStatus.DRAW_INSUFFICIENT_MATERIAL, GameStatus.DRAW_THREEFOLD_REPETITION);

        int winAsWhite = gameRepository.countByWhitePlayer_UsernameAndStatusIn(user.getName(), whiteWinStatuses).intValue();
        int winAsBlack = gameRepository.countByBlackPlayer_UsernameAndStatusIn(user.getName(), blackWinStatuses).intValue();
        int loseAsWhite = gameRepository.countByWhitePlayer_UsernameAndStatusIn(user.getName(), blackWinStatuses).intValue();
        int loseAsBlack = gameRepository.countByBlackPlayer_UsernameAndStatusIn(user.getName(), whiteWinStatuses).intValue();
        int drawAsWhite = gameRepository.countByWhitePlayer_UsernameAndStatusIn(user.getName(), drawStatuses).intValue();
        int drawAsBlack = gameRepository.countByBlackPlayer_UsernameAndStatusIn(user.getName(), drawStatuses).intValue();

        return ResponseEntity.ok(new ApiSuccessResponse<>(new UserStats(winAsWhite, winAsBlack, loseAsWhite, loseAsBlack, drawAsWhite, drawAsBlack)));
    }

    @GetMapping("/me/games")
    public ResponseEntity<ApiSuccessResponse<List<GameEntityResponse>>> getUserGames(Principal user){

        PageRequest limitRequest = PageRequest.of(0, 10, Sort.by("updated").descending());
        Page<GameEntity> gameEntityPage = gameRepository.findByWhitePlayer_UsernameOrBlackPlayer_Username(user.getName(), user.getName(), limitRequest);
        List<GameEntityResponse> response = gameEntityPage.getContent().stream().map(GameEntityResponse::new).toList();
        return ResponseEntity.ok(new ApiSuccessResponse<>(response));
    }
}
