package payment.app.transaction_service.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import payment.app.common_security.util.AESEncryptionUtil;
import payment.app.transaction_service.kafka.event.TransactionCompletedEvent;
import payment.app.transaction_service.kafka.event.TransactionReversedEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    public boolean publish(TransactionCompletedEvent event) {

        try {
            log.info("Publishing transaction event: {}", event.getTransactionId());

            // Convert object to JSON
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Original Payload: {}", jsonPayload);
            
            // Encrypt JSON
            String encryptedPayload  = aesEncryptionUtil.encrypt(jsonPayload);
            log.info("Encrypted Payload: {}", encryptedPayload);


            // 1. Capture the CompletableFuture returned by Spring Kafka
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(transactionCompletedTopic, event.getTransactionId(), encryptedPayload);

            // 2. Block the thread and wait for Kafka to return a physical Network Acknowledgment
            // We set a 5-second timeout so the thread doesn't hang indefinitely if the cluster is dead.
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);

            log.info("Kafka acknowledged transaction event! Topic={}, Partition={}, Offset={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

            return true;

            /*//Send encrypted payload
            kafkaTemplate.send(transactionCompletedTopic, event.getTransactionId(), encryptedPayload);
            log.info("Transaction event queued for Kafka publishing: {}", event.getTransactionId());*/

        } catch (InterruptedException ex) {
            // Keep this separate, so you can properly restore the thread state
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted while waiting for Kafka acknowledgment. TransactionId={}",
                    event.getTransactionId(),
                    ex);
            return false;

        } catch (JsonProcessingException | ExecutionException | TimeoutException ex) {
            // Combine the rest using the multi-catch '|' operator
            // We use ex.getCause() if it's an ExecutionException to print the real underlying Kafka error
            Throwable rootCause = (ex instanceof ExecutionException) ? ex.getCause() : ex;

            log.error("Failed to safely publish transaction event to Kafka. TransactionId={}, ErrorType={}, Reason={}",
                    event.getTransactionId(),
                    ex.getClass().getSimpleName(),
                    rootCause.getMessage());

            return false;
        }
    }

    public boolean publishReversal(TransactionReversedEvent event) {

        try {
            log.info("Publishing transaction reversal event: {}", event.getTransactionId());

            // Convert object to JSON
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Original reversal Payload: {}", jsonPayload);

            // Encrypt JSON
            String encryptedPayload = aesEncryptionUtil.encrypt(jsonPayload);
            log.info("Encrypted reversal Payload: {}", encryptedPayload);


            // 1. Capture the CompletableFuture returned by Spring Kafka
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(transactionReversedTopic, event.getTransactionId(), encryptedPayload);

            // 2. Block the thread and wait for Kafka to return a physical Network Acknowledgment
            // We set a 5-second timeout so the thread doesn't hang indefinitely if the cluster is dead.
            SendResult<String, String> result = future.get(5, TimeUnit.SECONDS);

            log.info("Kafka acknowledged transaction reversal event! Topic={}, Partition={}, Offset={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

            return true;


            /*//Send encrypted payload
            kafkaTemplate.send(transactionReversedTopic, event.getTransactionId(), encryptedPayload);
            log.info("Transaction reversal event queued for Kafka publishing: {}", event.getTransactionId());*/

        } catch (InterruptedException ex) {
            // Keep this separate, so you can properly restore the thread state
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted while waiting for Kafka acknowledgment. TransactionId={}",
                    event.getTransactionId(),
                    ex);
            return false;

        } catch (JsonProcessingException | ExecutionException | TimeoutException ex) {
            // Combine the rest using the multi-catch '|' operator
            // We use ex.getCause() if it's an ExecutionException to print the real underlying Kafka error
            Throwable rootCause = (ex instanceof ExecutionException) ? ex.getCause() : ex;

            log.error("Failed to safely publish transaction reversal event to Kafka. TransactionId={}, ErrorType={}, Reason={}",
                    event.getTransactionId(),
                    ex.getClass().getSimpleName(),
                    rootCause.getMessage());

            return false;
        }
    }
}
