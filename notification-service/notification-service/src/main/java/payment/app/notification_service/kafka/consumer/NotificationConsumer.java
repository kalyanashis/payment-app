package payment.app.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import payment.app.common_security.util.AESEncryptionUtil;
import payment.app.notification_service.kafka.event.TransactionCompletedEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AESEncryptionUtil aesEncryptionUtil;

    @KafkaListener(topics = "transaction-completed", groupId = "notification-group")
    public void consume(ConsumerRecord<String, String> consumerRecord) {

        try {
            // Read encrypted payload
            String encryptedPayload = consumerRecord.value();
            log.info("Encrypted Payload: {}", encryptedPayload);

            // Decrypt payload
            String decryptedJson = aesEncryptionUtil.decrypt(encryptedPayload);
            log.info("Decrypted JSON: {}", decryptedJson);

            // Convert JSON to Object
            TransactionCompletedEvent event = objectMapper.readValue(decryptedJson, TransactionCompletedEvent.class);

            log.info("Notification received for transaction={}", event.getTransactionId());
            log.info("Transferred ₹{} from {} to {}", event.getAmount(), event.getFromAccount(), event.getToAccount());

        } catch (Exception ex) {
            throw new RuntimeException("Failed to process encrypted Kafka message", ex);
        }
    }

        @KafkaListener(topics = "transaction-completed-dlt", groupId = "dlt-group-v3")
        public void consumeDlt(ConsumerRecord<String, String> consumerRecord) {

            try {
                // Read encrypted payload
                String encryptedPayload = consumerRecord.value();
                log.error("Received encrypted FAILED event in DLT: {}", encryptedPayload);

                // Decrypt payload
                String decryptedJson = aesEncryptionUtil.decrypt(encryptedPayload);

                // Convert JSON to Object
                TransactionCompletedEvent event = objectMapper.readValue(decryptedJson, TransactionCompletedEvent.class);
                log.error("Decrypted FAILED event from DLT: {}", event);

                // Re-encrypt before republishing
                String reEncryptedPayload = aesEncryptionUtil.encrypt(decryptedJson);
                kafkaTemplate.send("transaction-completed", event.getTransactionId(), reEncryptedPayload);
                log.info("Republished encrypted event back to original topic");

            } catch (Exception ex) {
                throw new RuntimeException("Failed to process DLT event", ex);
            }
        }

        /*@KafkaListener(topics = "transaction-completed-dlt", groupId = "dlt-group-v3")
        public void consumeDlt (TransactionCompletedEvent event){

            log.error("Received FAILED event in DLT: {}", event);

            kafkaTemplate.send("transaction-completed", event.getTransactionId(), event);

            log.info("Republished event back to original topic");
        }*/

}
