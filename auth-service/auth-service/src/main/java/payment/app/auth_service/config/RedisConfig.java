package payment.app.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // KEY SERIALIZER
        template.setKeySerializer(
                new StringRedisSerializer()
        );

        // VALUE SERIALIZER
        template.setValueSerializer(
                new StringRedisSerializer()
        );

        // HASH KEY SERIALIZER
        template.setHashKeySerializer(
                new StringRedisSerializer()
        );

        // HASH VALUE SERIALIZER
        template.setHashValueSerializer(
                new StringRedisSerializer()
        );

        template.afterPropertiesSet();

        return template;
    }
}
