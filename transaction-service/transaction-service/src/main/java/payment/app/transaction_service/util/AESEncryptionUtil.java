package payment.app.transaction_service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import payment.app.transaction_service.exception.EncryptionException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/*@Component
public class AESEncryptionUtil {

    private static final String ALGORITHM = "AES";

    @Value("${aes.secret}")
    private String secret;

    // Encrypt
    public String encrypt(String data) {

        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), ALGORITHM);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());

            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception ex) {
            throw new EncryptionException("Encryption failed", ex);
        }
    }

    // Decrypt
    public String decrypt(String encryptedData) {

        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), ALGORITHM);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes);

        } catch (Exception ex) {
            throw new EncryptionException("Decryption failed", ex);
        }
    }
}*/
