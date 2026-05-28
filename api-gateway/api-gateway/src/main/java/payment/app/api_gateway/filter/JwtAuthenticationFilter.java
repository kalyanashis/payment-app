package payment.app.api_gateway.filter;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import payment.app.api_gateway.service.RateLimiterService;
import payment.app.api_gateway.service.SlidingWindowRateLimiterService;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final SlidingWindowRateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        System.out.println("JWT Filter Hit");

        String path = exchange.getRequest().getURI().getPath();

        // PUBLIC ENDPOINTS
        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        System.out.println("Authorization: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String blacklisted = redisTemplate.opsForValue().get(token);

        if(blacklisted != null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return  exchange.getResponse().setComplete();
        }

        // Extract claims
        Claims claims = null;
        try {
            claims = jwtUtil.extractClaims(token);
            System.out.println("Claims: " + claims);
        } catch (Exception e) {
            System.out.println("JWT ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        // FOR RATE LIMITER
        if(path.equals("/transactions/transfer")) {

            System.out.println("The Transfer path is: " + exchange.getRequest().getURI().getPath());
            //boolean allowed = rateLimiterService.isAllowed(userId);

            /*if(!allowed) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }*/
        }

        // Add headers (Context Enrichment)
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

        // Continue filter chain with modified request
        //return chain.filter(exchange);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
