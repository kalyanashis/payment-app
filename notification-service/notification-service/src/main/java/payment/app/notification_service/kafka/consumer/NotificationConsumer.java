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
import payment.app.notification_service.kafka.event.TransactionReversedEvent;
import payment.app.notification_service.model.ProcessedEvent_old;
import payment.app.notification_service.repository.ProcessedEventRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AESEncryptionUtil aesEncryptionUtil;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "${app.kafka.topics.transaction-completed}", groupId = "notification-group")
    public void consume(ConsumerRecord<String, String> consumerRecord) {

        log.info("Processing started at {}", LocalDateTime.now());

        try {

            TransactionCompletedEvent event = decryptAndDeserialize(consumerRecord, TransactionCompletedEvent.class);

            log.info("Notification received for transaction={}", event.getTransactionId());

            if (processedEventRepository.existsByEventId(event.getTransactionId())) {
                log.info("Duplicate event received. Ignoring transaction={}", event.getTransactionId());
                return;
            }

            //Simulate consumer failure (for DLT testing purpose)
            if (event.getAmount().compareTo(BigDecimal.valueOf(1000)) >= 0) {
                throw new RuntimeException("Simulated notification failure");
            }

            log.info("Transferred ₹{} from {} to {}", event.getAmount(), event.getFromAccount(), event.getToAccount());

            processedEventRepository.save(new ProcessedEvent_old(event.getTransactionId()));
            log.info("Marked transaction={} as processed", event.getTransactionId());

        } catch (Exception ex) {
            throw new RuntimeException("Failed to process encrypted Kafka message", ex);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.transaction-reversed}", groupId = "notification-group")
    public void consumeReversal(ConsumerRecord<String, String> consumerRecord) {

        try {

            TransactionReversedEvent event = decryptAndDeserialize(consumerRecord, TransactionReversedEvent.class);

            log.info(
                    "Received transaction reversal event. Reversal Transaction: {}, Original Transaction: {}",
                    event.getTransactionId(),
                    event.getOriginalTransactionId()
            );
            log.info(
                    "Reversed ₹{} from {} to {}",
                    event.getAmount(),
                    event.getFromAccount(),
                    event.getToAccount()
            );

        } catch (Exception ex) {
            throw new RuntimeException("Failed to process transaction reversal event", ex);
        }
    }

    @KafkaListener(topics = "transaction-completed-dlt", groupId = "dlt-group")
    public void consumeDlt(ConsumerRecord<String, String> consumerRecord) {

        try {
            // Read encrypted payload
            String encryptedPayload = consumerRecord.value();
            log.error("Message moved to DLT. Topic={}, Key={}", consumerRecord.topic(), consumerRecord.key());

            // Decrypt payload
            String decryptedJson = aesEncryptionUtil.decrypt(encryptedPayload);

            // Convert JSON to Object
            TransactionCompletedEvent event = objectMapper.readValue(decryptedJson, TransactionCompletedEvent.class);
            log.error("Failed transaction event received from DLT: TransactionId={}", event.getTransactionId());
            log.error("Failed event received from DLT: {}", event);

            // DO NOT REPUBLISH
            log.info("DLT message logged successfully for investigation");

        } catch (Exception ex) {
            log.error("Failed to process DLT message", ex);
        }
    }

    @KafkaListener(topics = "transaction-reversed-dlt", groupId = "dlt-group")
    public void consumeReversedDlt(ConsumerRecord<String, String> consumerRecord) {

        try {
            // Read encrypted payload
            String encryptedPayload = consumerRecord.value();
            log.error("Message moved to DLT. Topic={}, Key={}", consumerRecord.topic(), consumerRecord.key());

            // Decrypt payload
            String decryptedJson = aesEncryptionUtil.decrypt(encryptedPayload);

            // Convert JSON to Object
            TransactionReversedEvent event = objectMapper.readValue(decryptedJson, TransactionReversedEvent.class);
            log.error("Transaction reversal event received from DLT: TransactionId={}", event.getTransactionId());
            log.error("Failed event received from DLT: {}", event);

            // DO NOT REPUBLISH
            log.info("DLT message logged successfully for investigation");

        } catch (Exception ex) {
            log.error("Failed to process DLT message", ex);
        }
    }

    private <T> T decryptAndDeserialize(ConsumerRecord<String, String> consumerRecord, Class<T> clazz) {

        try {
            // Read encrypted payload
            String encryptedPayload = consumerRecord.value();
            log.info("Encrypted payload for Kafka topic {}: {}", consumerRecord.topic(), encryptedPayload);

            // Decrypt payload
            String decryptedJson = aesEncryptionUtil.decrypt(encryptedPayload);
            log.info("Decrypted reversal JSON for Kafka topic {}: {}", consumerRecord.topic(), decryptedJson);

            return objectMapper.readValue(decryptedJson, clazz);

        } catch(Exception ex) {
            throw new RuntimeException("Failed to process encrypted Kafka message", ex);
        }
    }
}
