package payment.app.api_gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private static final int LIMIT = 5;
    private static final int WINDOW_SECONDS = 200;

    public boolean isAllowed(String userId) {

        String key = "rate_limit:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if(count == null) {
            return false;
        }

        // FIRST REQUEST
        if(count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }

        return count <= LIMIT;
    }
}
