package payment.app.transaction_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import payment.app.common_security.util.AESEncryptionUtil;

@Configuration
public class EncryptionConfig {

    @Value("${aes.secret}")
    private String secret;

    @Bean
    public AESEncryptionUtil aesEncryptionUtil() {
        return new AESEncryptionUtil(secret);
    }
}
