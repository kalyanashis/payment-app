package payment.app.transaction_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import payment.app.transaction_service.config.FeignConfig;
import payment.app.transaction_service.model.dto.AmountRequest;
import payment.app.transaction_service.model.dto.BalanceResponse;

@FeignClient(name = "account-service", url = "${services.account.url}", configuration = FeignConfig.class)
public interface AccountServiceClient {

    @PostMapping("accounts/{accountNumber}/debit")
    void debit(@PathVariable("accountNumber") String accountNumber,
               @RequestBody AmountRequest request,
               @RequestHeader("Authorization") String token);

    @PostMapping("accounts/{accountNumber}/credit")
    void credit(@PathVariable("accountNumber") String accountNumber,
                @RequestBody AmountRequest request,
                @RequestHeader("Authorization") String token);

    @GetMapping("/accounts/{accountNumber}/balance")
    BalanceResponse getBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestHeader("Authorization") String token
    );
}
