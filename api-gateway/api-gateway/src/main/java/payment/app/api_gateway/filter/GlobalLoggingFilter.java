package payment.app.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        log.info("Incoming request: {} {}", request.getMethod(), request.getURI());

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> logResponse(exchange, startTime))
                .doOnError(error -> logError(exchange, error, startTime));
    }

    private void logResponse(ServerWebExchange exchange, long startTime) {
        ServerHttpResponse response = exchange.getResponse();
        long duration = System.currentTimeMillis() - startTime;

        log.info("Outgoing response: status={} time={}", response.getStatusCode(), duration);
    }

    private void logError(ServerWebExchange exchange, Throwable error, long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        log.error("Request failed: path={} error={} time-{}ms", exchange.getRequest().getURI(), error.getMessage(), duration);

    }

    @Override
    public int getOrder() {
        return -1;
    }
}
