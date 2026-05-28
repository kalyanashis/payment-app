package payment.app.transaction_service.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import payment.app.transaction_service.model.dto.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "from_account")
    private String fromAccount;

    @Column(name = "to_account")
    private String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private String status; // SUCCESS / FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
        // JPA
    }

    public Transaction(String transactionId,
                       String fromAccount,
                       String toAccount,
                       BigDecimal amount,
                       TransactionType type,
                       String status) {

        this.transactionId = transactionId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public void assignIdempotencyKey(String key) {
        this.idempotencyKey = key;
    }
}
