package payment.app.account_service.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class AmountRequest {

    @NotNull
    @Positive
    private BigDecimal amount;
}
