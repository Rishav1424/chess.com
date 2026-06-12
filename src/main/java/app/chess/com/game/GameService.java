package app.chess.com.game;

import app.chess.com.exception.GameNotFoundException;
import app.chess.com.exception.InvalidActionException;
import app.chess.com.exception.UnauthorizedGameAccessException;
import app.chess.com.dto.GameEntityResponse;
import app.chess.com.dto.GameStatusResponse;
import app.chess.com.user.UserRepository;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.jetbrains.annotations.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GameService {

    @Autowired
    GameRepository gameRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedisTemplate<String, GameState> gameStateRedisTemplate;

    @Autowired
    RedisTemplate<String, String> stringRedisTemplate;

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    public static final String STATE_PREFIX = "GameSate: ";
    public static final String MOVE_PREFIX = "GameMoves: ";
    public static final String DRAW_PREFIX = "DrawRequest: ";
    public static final String TIMEOUT_SET_KEY = "GameTimeouts";
    public static final Duration DRAW_OFFER_DURATION = Duration.ofSeconds(30);
    public static final Duration TOTAL_TIME = Duration.ofMinutes(3); //Total duration
    public static final Duration BONUS_PER_MOVE = Duration.ofSeconds(5);

    public GameEntityResponse getGame(Long gameId) {
        GameEntity game = gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
        return new GameEntityResponse(game);
    }

    public GameStatusResponse getGameStatus(Long gameId) {
        GameState state = gameStateRedisTemplate.opsForValue().get(STATE_PREFIX + gameId);
        if (state == null) throw new GameNotFoundException(gameId);
        List<String> moves = stringRedisTemplate.opsForList().range(MOVE_PREFIX + gameId, 0, -1);
        Duration timeElapsed = Duration.between(state.getLastMoveTime(), Instant.now());
        if(moves == null) throw new GameNotFoundException(gameId);
        if (moves.size() % 2 == 0) {
            state.setWhiteTime(state.getWhiteTime().minus(timeElapsed));
        } else {
            state.setBlackTime(state.getBlackTime().minus(timeElapsed));
        }
        return new GameStatusResponse(state.getFen(), state.getWhitePlayer(), state.getBlackPlayer(), state.getWhiteTime(), state.getBlackTime(), moves.toArray(String[]::new));
    }

    public Long createGame(String whiteUserName, String blackUserName) {
        GameEntity game = new GameEntity();
        game.setWhitePlayer(userRepository.findByUsername(whiteUserName).orElseThrow());
        game.setBlackPlayer(userRepository.findByUsername(blackUserName).orElseThrow());
        gameRepository.save(game);
        Long id = game.getId();

        GameState gameState = new GameState(whiteUserName, blackUserName);
        gameStateRedisTemplate.opsForValue().set(STATE_PREFIX + id, gameState);
        Instant timeoutTimestamp = Instant.now().plus(gameState.getWhiteTime());
        stringRedisTemplate.opsForZSet().add(TIMEOUT_SET_KEY, id.toString(), timeoutTimestamp.toEpochMilli());

        return id;
    }

    public void makeMove(Long gameId, String inputMove, String playerName) {
        GameState gameState = gameStateRedisTemplate.opsForValue().get(STATE_PREFIX + gameId);
        if (gameState == null) throw new GameNotFoundException(gameId);
        boolean isWhitePlayer = playerName.equals(gameState.getWhitePlayer());
        boolean isBlackPlayer = playerName.equals(gameState.getBlackPlayer());

        log.info("Game {} for player {} (white: {}, black: {})", gameId, playerName, gameState.getWhitePlayer(), gameState.getBlackPlayer());

        Board board = new Board();
        board.loadFromFen(gameState.getFen());

        if (!isBlackPlayer && !isWhitePlayer) {
            throw new UnauthorizedGameAccessException();
        }
        if (board.getSideToMove().equals(Side.WHITE) && !isWhitePlayer) {
            throw new InvalidActionException("Not your turn");
        }
        if (board.getSideToMove().equals(Side.BLACK) && !isBlackPlayer) {
            throw new InvalidActionException("Not your turn");
        }

        Move move = new Move(inputMove, board.getSideToMove());

        log.info("Player {} making move {} in game {} having state {} for side {}", playerName, inputMove, gameId, board.getFen(), board.getSideToMove());
        boolean success = board.doMove(move, true);
        log.info("Changed state to {} and side to {} successfully {}", board.getFen(), board.getSideToMove(), success);
        if (!success) {
            throw new InvalidActionException("Invalid Move:" + inputMove);
        }

        gameState.setFen(board.getFen());

        Duration timeElapsed = Duration.between(gameState.getLastMoveTime(), Instant.now());
        gameState.setLastMoveTime(Instant.now());

        if (isWhitePlayer) {
            gameState.setWhiteTime(gameState.getWhiteTime().minus(timeElapsed).plus(BONUS_PER_MOVE));
            if (gameState.getWhiteTime().isNegative() || gameState.getWhiteTime().isZero()) {
                handleGameOver(gameId, GameStatus.WON_BLACK_TIMEOUT);
                return;
            }
        } else {
            gameState.setBlackTime(gameState.getBlackTime().minus(timeElapsed).plus(BONUS_PER_MOVE));
            if (gameState.getBlackTime().isNegative() || gameState.getBlackTime().isZero()) {
                handleGameOver(gameId, GameStatus.WON_WHITE_TIMEOUT);
                return;
            }
        }

        gameStateRedisTemplate.opsForValue().set(STATE_PREFIX + gameId, gameState);
        stringRedisTemplate.opsForList().rightPush(MOVE_PREFIX + gameId, inputMove);

        Duration remainingDuration = isWhitePlayer ? gameState.getBlackTime() : gameState.getWhiteTime();
        Instant timeoutTimestamp = Instant.now().plus(remainingDuration);
        stringRedisTemplate.opsForZSet().add(TIMEOUT_SET_KEY, gameId.toString(), timeoutTimestamp.toEpochMilli());

        simpMessagingTemplate.convertAndSend(String.format("/topic/game/%d/move", gameId), move.toString());

        checkGameOver(gameId, board);
    }

    private void checkGameOver(Long gameId, @NotNull Board board) {
        GameStatus status = GameStatus.ONGOING;
        if (board.isMated()) {
            status = (board.getSideToMove() == Side.BLACK) ? GameStatus.WON_WHITE_CHECKMATE : GameStatus.WON_BLACK_CHECKMATE;
        }
        if (board.isStaleMate()) {
            status = GameStatus.DRAW_STALEMATE;
        }
        if (board.isRepetition()) {
            status = GameStatus.DRAW_THREEFOLD_REPETITION;
        }
        if (board.isInsufficientMaterial()) {
            status = GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (board.getHalfMoveCounter() > 100) {
            status = GameStatus.DRAW_FIFTY_MOVE_RULE;
        }
        if (!status.equals(GameStatus.ONGOING)) {
            handleGameOver(gameId, status);
        }
    }

    private void handleGameOver(Long gameId, GameStatus status) {
        log.info("Game {} ended with status {}", gameId, status);
        List<String> moves = stringRedisTemplate.opsForList().range(MOVE_PREFIX + gameId, 0, -1);
        GameEntity gameEntity = gameRepository.findById(gameId).orElseThrow();
        gameEntity.setStatus(status);
        if (moves != null) gameEntity.setMoves(moves.toArray(new String[0]));
        gameRepository.save(gameEntity);
        gameStateRedisTemplate.delete(STATE_PREFIX + gameId);
        stringRedisTemplate.delete(MOVE_PREFIX + gameId);
        stringRedisTemplate.opsForZSet().remove(TIMEOUT_SET_KEY, gameId.toString());
        simpMessagingTemplate.convertAndSend(String.format("/topic/game/%d/event", gameId), status.toString());
    }

    public void handleResignation(Long gameId, String playerName) {
        GameState gameState = gameStateRedisTemplate.opsForValue().get(STATE_PREFIX + gameId);
        if (gameState == null) throw new GameNotFoundException(gameId);

        if (playerName.equals(gameState.getWhitePlayer())) {
            handleGameOver(gameId, GameStatus.WON_BLACK_RESIGNATION);
        } else if (playerName.equals(gameState.getBlackPlayer())) {
            handleGameOver(gameId, GameStatus.WON_WHITE_RESIGNATION);
        } else throw new UnauthorizedGameAccessException();
    }

    public void handleDrawOffer(Long gameId, String playerName) {
        GameState gameState = gameStateRedisTemplate.opsForValue().get(STATE_PREFIX + gameId);
        if (gameState == null) throw new GameNotFoundException(gameId);

        if (playerName.equals(stringRedisTemplate.opsForValue().get(DRAW_PREFIX + gameId))) {
            handleGameOver(gameId, GameStatus.DRAW_AGREEMENT);
            return;
        }

        String opponent;
        String event;
        if (playerName.equals(gameState.getWhitePlayer())) {
            opponent = gameState.getBlackPlayer();
            event = "WHITE_DRAW_REQUEST";
        } else if (playerName.equals(gameState.getBlackPlayer())) {
            opponent = gameState.getWhitePlayer();
            event = "BLACK_DRAW_REQUEST";
        } else {
            throw new UnauthorizedGameAccessException();
        }
        stringRedisTemplate.opsForValue().set(DRAW_PREFIX + gameId, opponent, DRAW_OFFER_DURATION);
        simpMessagingTemplate.convertAndSend(String.format("/topic/game/%d/event", gameId), event);
    }

    @Scheduled(fixedRate = 1000) // Poll Redis once per second
    public void sweepTimeouts() {
        Instant now = Instant.now();

        Set<String> timedOutGames = stringRedisTemplate.opsForZSet().rangeByScore(TIMEOUT_SET_KEY, 0, now.toEpochMilli());

        if(timedOutGames == null) throw new IllformedLocaleException("Unable to fetch timedOut games");

        for (String gameIdStr : timedOutGames) {
            Long gameId = Long.parseLong(gameIdStr);
            stringRedisTemplate.opsForZSet().remove(TIMEOUT_SET_KEY, gameIdStr);
            processTimeout(gameId);
        }

    }

    public void processTimeout(Long gameId) {
        GameState gameState = gameStateRedisTemplate.opsForValue().get(STATE_PREFIX + gameId);
        if (gameState == null) return;
        boolean isWhiteTurn = gameState.getFen().split(" ")[1].equals("w");
        log.info("Processing timeout for game {}. White's turn: {}", gameId, isWhiteTurn);
        GameStatus result = isWhiteTurn ? GameStatus.WON_BLACK_TIMEOUT : GameStatus.WON_WHITE_TIMEOUT;
        handleGameOver(gameId, result);
    }
}