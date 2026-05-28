package payment.app.account_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import payment.app.account_service.model.entity.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);
}
