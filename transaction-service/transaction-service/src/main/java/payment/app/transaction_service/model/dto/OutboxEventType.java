package payment.app.transaction_service.model.dto;

public enum OutboxEventType {
    TRANSACTION_COMPLETED,
    TRANSACTION_REVERSED
}
