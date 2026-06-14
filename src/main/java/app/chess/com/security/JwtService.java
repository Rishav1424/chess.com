package app.chess.com.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final String jwtSecretKey;
    private final long jwtExpirationMs;

    // Inject values from application.properties
    public JwtService(@Value("${jwt.secret-key}") String jwtSecretKey, @Value("${jwt.expiration-ms}") long jwtExpirationMs) {
        this.jwtSecretKey = jwtSecretKey;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Extracts the username from a JWT token.
     *
     * @param token The JWT token.
     * @return The username contained within the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from a JWT token using a resolver function.
     *
     * @param token          The JWT token.
     * @param claimsResolver A function to extract the desired claim.
     * @param <T>            The type of the claim.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for the given UserDetails.
     *
     * @param userDetails The user details to include in the token.
     * @return The generated JWT token string.
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtExpirationMs);
    }

    /**
     * Builds the JWT token with specified claims, subject, and expiration.
     *
     * @param userDetails The user details (subject).
     * @param expiration  The expiration time in milliseconds.
     * @return The generated JWT token string.
     */
    private String buildToken(UserDetails userDetails, long expiration) {
        return Jwts.builder().setSubject(userDetails.getUsername()).setIssuedAt(new Date(System.currentTimeMillis())).setExpiration(new Date(System.currentTimeMillis() + expiration)).signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
    }

    /**
     * Validates if a JWT token is valid for the given UserDetails.
     * Checks if the username matches and the token is not expired.
     *
     * @param token       The JWT token.
     * @param userDetails The user details to validate against.
     * @return True if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks if a JWT token has expired.
     *
     * @param token The JWT token.
     * @return True if the token is expired, false otherwise.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token The JWT token.
     * @return The expiration date.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parses all claims from a JWT token.
     *
     * @param token The JWT token.
     * @return The Claims object containing all data.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
    }

    /**
     * Return the expiration instant from a JWT token.
     *
     * @param token The JWT token.
     * @return The expiration instant.
     */
    public Instant getExpiration(String token) {
        return extractExpiration(token).toInstant();
    }

    /**
     * Generates the signing key from the base64 encoded secret.
     *
     * @return The signing key.
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

