package app.chess.com.user.dtos;

public record UserStats(int winAsWhite, int winAsBlack, int loseAsWhite, int loseAsBlack, int drawAsWhite, int drawAsBlack) {
}
