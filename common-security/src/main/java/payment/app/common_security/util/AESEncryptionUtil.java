package payment.app.common_security.util;

import payment.app.common_security.exception.EncryptionException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESEncryptionUtil {

    private static final String ALGORITHM = "AES";

    private final String secret;

    public AESEncryptionUtil(String secret) {
        this.secret = secret;
    }


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
}
