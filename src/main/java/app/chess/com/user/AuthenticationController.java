package app.chess.com.user;

import app.chess.com.security.JwtService;
import app.chess.com.user.dtos.LoginRequest;
import app.chess.com.user.dtos.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller handling user authentication (login) and potentially registration.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserRepository userRepository; // Optional: For registration

    @Autowired
    PasswordEncoder passwordEncoder; // Optional: For registration

    /**
     * Handles the login request. Authenticates the user and returns a JWT token
     * if successful.
     * @param request The LoginRequest DTO containing username and password.
     * @return ResponseEntity containing LoginResponse with the JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        try{

            // Perform authentication using Spring Security's AuthenticationManager
            // This will use our DatabaseUserDetailsService and PasswordEncoder

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            // If authentication is successful, the principal is our UserDetails object
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Generate the JWT token
            String token = jwtService.generateToken(userDetails);
            // Return the token in the response
            return ResponseEntity.ok(token);
        }catch (Exception e){
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest request) {
        try{

        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken!");
        }

        // Create new user entity
        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setEmail(request.email()); // Assuming RegisterRequest has email
        newUser.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(newUser);

        return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(String.format("Registration failed: %s", e.getMessage()));
        }
        // Check if username already exists
    }
}
