package app.chess.com.user;

import app.chess.com.dto.*;
import app.chess.com.game.GameEntity;
import app.chess.com.game.GameRepository;
import app.chess.com.game.GameStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    GameRepository gameRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiSuccessResponse<UserProfileResponse>> getProfile(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new UsernameNotFoundException(principal.getName()));
        return ResponseEntity.ok(new ApiSuccessResponse<>(new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail())));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<ApiSuccessResponse<UserStats>> getUserStats(Principal user) {

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
    public ResponseEntity<ApiSuccessResponse<PageResponse<GameEntityResponse>>> getUserGames(Principal user, @RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "0") int offset) {

        int safeLimit = Math.min(Math.max(limit, 1), 50);
        int pageNumber = offset / safeLimit;

        PageRequest pageRequest = PageRequest.of(pageNumber, safeLimit, Sort.by("updated").descending());
        Page<GameEntity> gameEntityPage = gameRepository.findByWhitePlayer_UsernameOrBlackPlayer_Username(user.getName(), user.getName(), pageRequest);
        List<GameEntityResponse> items = gameEntityPage.getContent().stream().map(GameEntityResponse::new).toList();
        PageResponse<GameEntityResponse> response = new PageResponse<>(items, new PageResponse.PageInfo(safeLimit, offset, gameEntityPage.getTotalElements()));
        return ResponseEntity.ok(new ApiSuccessResponse<>(response));
    }
}
