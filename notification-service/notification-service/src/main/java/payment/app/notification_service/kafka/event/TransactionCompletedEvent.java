package payment.app.notification_service.kafka.event;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransactionCompletedEvent {

    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String status;
}
