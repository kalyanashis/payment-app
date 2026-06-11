package payment.app.notification_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReversedEvent {

    private String transactionId;
    private String originalTransactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String status;
}
