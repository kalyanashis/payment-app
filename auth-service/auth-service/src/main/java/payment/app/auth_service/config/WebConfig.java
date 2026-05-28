package payment.app.auth_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import payment.app.auth_service.interceptor.GatewayValidationInterceptor;

@Component
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final GatewayValidationInterceptor gatewayValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(gatewayValidationInterceptor);
    }
}
