package payment.app.transaction_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BalanceResponse {

    private final String accountNumber;
    private final BigDecimal balance;
}
