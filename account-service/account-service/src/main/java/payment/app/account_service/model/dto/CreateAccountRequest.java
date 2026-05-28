package payment.app.account_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CreateAccountRequest {

    @NotBlank
    private String customerName;

    @NotNull
    @Positive
    private BigDecimal initialBalance;
}
