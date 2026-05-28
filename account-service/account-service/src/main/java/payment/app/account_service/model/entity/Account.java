package payment.app.account_service.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private String status; // ACTIVE, BLOCKED, CLOSED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Account(String accountNumber, String customerName, BigDecimal balance, String userId) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.balance = balance;
        this.userId = userId;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    // business methods
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}
