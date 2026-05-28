package payment.app.transaction_service.model.dto;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class TransactionResponse implements Serializable {

    private final String transactionId;
    private final String fromAccount;
    private final String toAccount;
    private final BigDecimal amount;
    private final TransactionType type;
    private final String status;
    private final LocalDateTime timestamp;

    private final boolean replay;

    public TransactionResponse(String transactionId,
                               String fromAccount,
                               String toAccount,
                               BigDecimal amount,
                               TransactionType type,
                               String status,
                               LocalDateTime timestamp,
                               boolean replay) {
        this.transactionId = transactionId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.timestamp = timestamp;
        this.replay = replay;
    }
}
