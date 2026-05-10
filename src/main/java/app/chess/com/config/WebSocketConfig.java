package app.chess.com.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    ChannelInterceptor webSocketAuthInterceptor;

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        return messages
                .simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
                .simpTypeMatchers(SimpMessageType.SUBSCRIBE).authenticated()
                .simpTypeMatchers(SimpMessageType.UNSUBSCRIBE, SimpMessageType.DISCONNECT).permitAll()
                .simpTypeMatchers(SimpMessageType.HEARTBEAT, SimpMessageType.OTHER).permitAll()
                .simpDestMatchers("/app/**").authenticated()
                .anyMessage().denyAll()
                .build();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Bean("csrfChannelInterceptor")
    public ChannelInterceptor csrfChannelInterceptor() {
        return webSocketAuthInterceptor;
    }

}

