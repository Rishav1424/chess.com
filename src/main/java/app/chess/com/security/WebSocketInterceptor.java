package app.chess.com.security;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class WebSocketInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        assert accessor != null;
        if (Objects.equals(accessor.getCommand(), StompCommand.CONNECT)) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                String username = jwtService.extractUsername(jwt);

                if (username != null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        accessor.setUser(authToken);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("WebSocket user {} connected", username);
                    } else {
                        log.warn("Invalid JWT token for WebSocket connection");
                        throw new IllegalArgumentException("Invalid JWT token");
                    }
                } else {
                    log.warn("Username not found in JWT token for WebSocket connection");
                    throw new IllegalArgumentException("Username not found in JWT token");
                }
            } else {
                log.warn("No JWT token found in WebSocket connection");
                throw new IllegalArgumentException("No JWT token found");
            }
        }
        return message;
    }
}