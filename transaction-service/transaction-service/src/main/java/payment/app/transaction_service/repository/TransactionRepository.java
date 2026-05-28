package payment.app.transaction_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import payment.app.transaction_service.model.entity.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountOrToAccount(String fromAccount, String toAccount);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
