package payment.app.auth_service.service.impl;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import payment.app.auth_service.model.dto.AuthResponse;
import payment.app.auth_service.model.dto.LoginRequest;
import payment.app.auth_service.model.dto.RegisterRequest;
import payment.app.auth_service.model.dto.RegisterResponse;
import payment.app.auth_service.model.entity.User;
import payment.app.auth_service.repository.UserRepository;
import payment.app.auth_service.security.JwtUtil;
import payment.app.auth_service.service.AuthService;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if(userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("User already exists");
        }

        User user = User.builder().username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();
        userRepository.save(user);
        return new RegisterResponse("User registered successfully");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token);
    }

    @Override
    public void logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);

        Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        redisTemplate.opsForValue().set("blacklist:" + token, "true", Duration.ofMillis(remainingMillis));

        String value = redisTemplate.opsForValue().get("blacklist:" + token);
        log.info("VALUE AFTER STORE = {}", value);
    }
}
