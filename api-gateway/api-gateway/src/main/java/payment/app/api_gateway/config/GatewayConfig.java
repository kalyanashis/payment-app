package payment.app.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class GatewayConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .build();
    }

    @Bean
    RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                    RedisRateLimiter redisRateLimiter,
                                    KeyResolver userKeyResolver) {

        return builder.routes()

                //Auth Service
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://localhost:8081"))

                //Account Service
                .route("account-service", r -> r
                        .path("/accounts/**")
                        .uri("http://localhost:8082"))

                //Transaction Service
                .route("transaction-service", r -> r
                        .path("/transactions/**")
                        .filters(f -> f.requestRateLimiter(
                                c -> {
                                    c.setRateLimiter(redisRateLimiter);
                                    c.setKeyResolver(userKeyResolver);
                                }
                        ))
                        .uri("http://localhost:8083"))
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {

        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {

        return exchange -> Mono.justOrEmpty(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst("X-User-Id")
        );
    }
}
