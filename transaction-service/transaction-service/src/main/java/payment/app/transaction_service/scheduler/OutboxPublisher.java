package payment.app.transaction_service.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import payment.app.transaction_service.kafka.event.TransactionCompletedEvent;
import payment.app.transaction_service.kafka.event.TransactionReversedEvent;
import payment.app.transaction_service.kafka.producer.TransactionEventProducer;
import payment.app.transaction_service.model.dto.OutboxStatusType;
import payment.app.transaction_service.model.entity.OutboxEvent;
import payment.app.transaction_service.repository.OutboxEventRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionEventProducer eventProducer;

    @Scheduled(fixedDelay = 10000)
    public void publishPendingEvents() {

        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatusType.PENDING);
        log.info("Found {} pending outbox event(s)", pendingEvents.size());

        for(OutboxEvent outboxEvent : pendingEvents) {

            try {

                log.info(
                        "Processing Outbox Event. EventId={}, EventType={}",
                        outboxEvent.getEventId(),
                        outboxEvent.getEventType()
                );

                switch(outboxEvent.getEventType()) {
                    case TRANSACTION_COMPLETED -> {
                        TransactionCompletedEvent event = objectMapper.readValue(
                                outboxEvent.getPayload(),
                                TransactionCompletedEvent.class);

                        log.info("Deserialized TransactionCompletedEvent: {}", event);

                        // 1. Pass data to the updated blocking method
                        boolean success = eventProducer.publish(event);

                        if (success) {
                            outboxEvent.markAsPublished();
                            outboxEventRepository.save(outboxEvent);
                            log.info("Published Outbox Event for transaction completion. EventId={}",
                                    outboxEvent.getEventId()
                            );
                        } else {
                            log.warn(
                                    "Kafka delivery for transaction event failed for EventId={}. " +
                                            "Event remains PENDING and will be retried in the next scheduler run.",
                                    outboxEvent.getEventId()
                            );
                            // CRITICAL: We break out of the processing loop entirely.
                            // We do NOT call markAsPublished(). The record stays PENDING.
                            // The next scheduler run (10 seconds later) will attempt to send this exact message again.
                            return;
                        }

                    }
                    case TRANSACTION_REVERSED -> {
                        TransactionReversedEvent event = objectMapper.readValue(
                                outboxEvent.getPayload(),
                                TransactionReversedEvent.class);

                        log.info("Deserialized TransactionReversedEvent: {}", event);

                        boolean success = eventProducer.publishReversal(event);

                        if (success) {
                            outboxEvent.markAsPublished();
                            outboxEventRepository.save(outboxEvent);
                            log.info("Published Outbox Event for transaction reversal completion. EventId={}",
                                    outboxEvent.getEventId()
                            );
                        } else {
                            log.warn(
                                    "Kafka delivery for transaction reversal event failed for EventId={}. " +
                                            "Event remains PENDING and will be retried in the next scheduler run.",
                                    outboxEvent.getEventId()
                            );
                            // CRITICAL: We break out of the processing loop entirely.
                            // We do NOT call markAsPublished(). The record stays PENDING.
                            // The next scheduler run (10 seconds later) will attempt to send this exact message again.
                            return;
                        }

                    }
                    default -> log.warn("Unknown Outbox Event Type: {}", outboxEvent.getEventType());
                }

            } catch(JsonProcessingException ex) {
                log.error("Failed to deserialize Outbox Event. EventId={}", outboxEvent.getEventId(), ex);
            }
        }
    }

}
