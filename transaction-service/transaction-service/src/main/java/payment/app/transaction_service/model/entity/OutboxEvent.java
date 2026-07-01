package payment.app.transaction_service.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import payment.app.transaction_service.model.dto.OutboxEventType;
import payment.app.transaction_service.model.dto.OutboxStatusType;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatusType status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected OutboxEvent() {
        // Required by JPA
    }

    public OutboxEvent(
            String eventId,
            OutboxEventType eventType,
            String payload,
            OutboxStatusType status,
            LocalDateTime createdAt) {

        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void markAsPublished() {
        this.status = OutboxStatusType.PUBLISHED;
    }
}
