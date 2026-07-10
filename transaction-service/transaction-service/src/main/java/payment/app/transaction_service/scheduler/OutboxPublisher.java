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

                        eventProducer.publish(event);

                        outboxEvent.markAsPublished();
                        outboxEventRepository.save(outboxEvent);

                        log.info("Published Outbox Event for transaction completion. EventId={}",
                                outboxEvent.getEventId());
                    }
                    case TRANSACTION_REVERSED -> {
                        TransactionReversedEvent event = objectMapper.readValue(
                                outboxEvent.getPayload(),
                                TransactionReversedEvent.class);

                        log.info("Deserialized TransactionReversedEvent: {}", event);

                        eventProducer.publishReversal(event);

                        outboxEvent.markAsPublished();
                        outboxEventRepository.save(outboxEvent);

                        log.info("Published Outbox Event for transaction reversal. EventId={}",
                                outboxEvent.getEventId());
                    }
                    default -> log.warn("Unknown Outbox Event Type: {}", outboxEvent.getEventType());
                }

            } catch(JsonProcessingException ex) {
                log.error("Failed to deserialize Outbox Event. EventId={}", outboxEvent.getEventId(), ex);
            }
        }
    }

}
