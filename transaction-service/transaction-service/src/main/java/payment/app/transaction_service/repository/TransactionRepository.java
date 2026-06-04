package payment.app.transaction_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import payment.app.transaction_service.model.dto.TransactionType;
import payment.app.transaction_service.model.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountOrToAccount(String fromAccount, String toAccount);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.fromAccount = :accountNumber " +
            "AND t.type = :type " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt >= :startOfDay")
    BigDecimal getTodayTransferAmount(
            @Param("accountNumber") String accountNumber,
            @Param("type") TransactionType type,
            @Param("startOfDay") LocalDateTime startOfDay
    );
}
