package payment.app.auth_service.service;

import jakarta.servlet.http.HttpServletRequest;
import payment.app.auth_service.model.dto.AuthResponse;
import payment.app.auth_service.model.dto.LoginRequest;
import payment.app.auth_service.model.dto.RegisterRequest;
import payment.app.auth_service.model.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register (RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(HttpServletRequest request);
}
