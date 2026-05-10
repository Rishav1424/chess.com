package app.chess.com.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * A Data Transfer Object (DTO) representing the JSON body expected
 * for a login request. A Java 17 "record" is a concise way
 * to create immutable data carriers.
 */
public record RegisterRequest(
        @NotBlank(message = "Name is mandatory")
        String username,

        @NotBlank(message = "Email is mandatory") @Email
        String email,

        @NotBlank(message = "Password is mandatory")
        @Min(value = 4, message = "Password must be at least 4 characters long")
        String password
) {
}