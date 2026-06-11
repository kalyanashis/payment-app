package payment.app.transaction_service.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import payment.app.common_security.util.AESEncryptionUtil;
import payment.app.transaction_service.kafka.event.TransactionCompletedEvent;
import payment.app.transaction_service.kafka.event.TransactionReversedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AESEncryptionUtil aesEncryptionUtil;

    @Value("${app.kafka.topics.transaction-completed}")
    private String transactionCompletedTopic;

    @Value("${app.kafka.topics.transaction-reversed}")
    private String transactionReversedTopic;

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
            kafkaTemplate.send(transactionCompletedTopic, event.getTransactionId(), encryptedPayload);
            log.info("Transaction event published successfully: {}", event.getTransactionId());

        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize event", ex);
        }
    }

    public void publishReversal(TransactionReversedEvent event) {

        try {
            log.info("Publishing transaction reversal event: {}", event.getTransactionId());

            // Convert object to JSON
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Original reversal Payload: {}", jsonPayload);

            // Encrypt JSON
            String encryptedPayload  = aesEncryptionUtil.encrypt(jsonPayload);
            log.info("Encrypted reversal Payload: {}", encryptedPayload);

            //Send encrypted payload
            kafkaTemplate.send(transactionReversedTopic, event.getTransactionId(), encryptedPayload);
            log.info("Transaction reversal event published successfully: {}", event.getTransactionId());

        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize transaction reversal event", ex);
        }
    }
}
