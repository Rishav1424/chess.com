package app.chess.com.user.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Name is mandatory")
        String username,

        @NotBlank(message = "Password is mandatory")
        String password) {
}
