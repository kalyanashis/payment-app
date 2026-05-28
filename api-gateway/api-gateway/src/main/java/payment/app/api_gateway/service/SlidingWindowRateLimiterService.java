package payment.app.api_gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlidingWindowRateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private static final int LIMIT = 5;
    private static final int WINDOW_SECONDS = 200;

    public boolean isAllowed(String userId) {

        String key = "rate_limit:" + userId;
        System.out.println("Rate Limit User: " + userId);
        long now = System.currentTimeMillis();
        long windowStart = now - (WINDOW_SECONDS * 1000L);

        ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();

        // remove old timestamps
        zSet.removeRangeByScore(key, 0, windowStart);

        // add current request timestamp
        zSet.add(key, UUID.randomUUID().toString(), now);

        // count remaining requests
        Long count = zSet.zCard(key);
        System.out.println("Current Count before limit check: " + count);
        if(count != null && count > LIMIT) {
            return false;
        }

        // set expiry
        redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));

        return true;
    }
}
