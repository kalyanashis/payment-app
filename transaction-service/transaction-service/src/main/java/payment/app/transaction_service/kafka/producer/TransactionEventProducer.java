package payment.app.transaction_service.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import payment.app.common_security.util.AESEncryptionUtil;
import payment.app.transaction_service.kafka.event.TransactionCompletedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AESEncryptionUtil aesEncryptionUtil;

    public void publish(TransactionCompletedEvent event) {

        try {
            log.info("Publishing transaction event: {}", event.getTransactionId());

            // Convert object to JSON
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Original Payload: {}", jsonPayload);
            
            // Encrypt JSON
            String encryptedPayload  = aesEncryptionUtil.encrypt(jsonPayload);
            log.info("Encrypted Payload: {}", encryptedPayload);

            //Send encrypted payload
            kafkaTemplate.send("transaction-completed", event.getTransactionId(), encryptedPayload);

        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize event", ex);
        }
    }
}
