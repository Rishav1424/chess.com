package app.chess.com.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig<K, V> {

    @Bean
    public RedisTemplate<K, V> redisTemplate(RedisConnectionFactory connectionFactory) {

        // 1. Configure the ObjectMapper first
        ObjectMapper objectMapper = new ObjectMapper();

        // This is necessary to fix the 'java.time.Duration not supported' error
        objectMapper.registerModule(new JavaTimeModule());

        // Optional: Ensure all dates are written as ISO 8601 strings, not timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // This tells Jackson to include the Java class name (type hint) in the JSON payload
        // using the @class property. This is what prevents deserialization to LinkedHashMap.
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 2. Create the serializer, passing the configured ObjectMapper into the constructor
        //    This replaces the deprecated setter call.
        return getRedisTemplate(connectionFactory, objectMapper);
    }

    private static <K, V> RedisTemplate<K, V> getRedisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        RedisTemplate<K, V> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Apply the serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}