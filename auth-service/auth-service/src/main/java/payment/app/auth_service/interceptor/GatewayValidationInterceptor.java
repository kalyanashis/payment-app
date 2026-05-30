package payment.app.auth_service.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class GatewayValidationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {


        log.info("Interceptor hit, in auth-service");

        String gatewayHeader = request.getHeader("X-Internal-Gateway");
        String internalServiceHeader = request.getHeader("X-Internal-Service");

        boolean allowed = "true".equals(gatewayHeader) || "true".equals(internalServiceHeader);

        if(!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Direct access forbidden");
            return false;
        }
        return true;
    }
}
