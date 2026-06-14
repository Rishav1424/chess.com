package app.chess.com.user;

import app.chess.com.dto.*;
import app.chess.com.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller handling user authentication (login) and potentially registration.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserRepository userRepository; // Optional: For registration

    @Autowired
    PasswordEncoder passwordEncoder; // Optional: For registration

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        AuthResponse body = new AuthResponse(token, "Bearer", jwtService.getExpiration(token), new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail()));
        return ResponseEntity.ok(new ApiSuccessResponse<>(body));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DataIntegrityViolationException("Username is already taken.");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DataIntegrityViolationException("Email address is already registered.");
        }

        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(newUser);

        String token = jwtService.generateToken(newUser);
        AuthResponse body = new AuthResponse(token, "Bearer", jwtService.getExpiration(token), new UserProfileResponse(newUser.getId(), newUser.getUsername(), newUser.getEmail()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiSuccessResponse<>(body));
    }
}
