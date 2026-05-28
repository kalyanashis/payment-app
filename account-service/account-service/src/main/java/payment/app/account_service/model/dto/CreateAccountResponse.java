package payment.app.account_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CreateAccountResponse {

    private final String accountNumber;
    private final BigDecimal balance;
    private final String status;
}
