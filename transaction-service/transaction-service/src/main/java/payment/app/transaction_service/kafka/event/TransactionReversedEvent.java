package payment.app.transaction_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionReversedEvent {

    private String transactionId;
    private String originalTransactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String status;
}
