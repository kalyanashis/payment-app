package payment.app.transaction_service.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AmountRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    public AmountRequest(BigDecimal amount) {
        this.amount = amount;
    }
}
