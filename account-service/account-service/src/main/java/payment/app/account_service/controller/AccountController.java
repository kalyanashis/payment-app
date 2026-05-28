package payment.app.account_service.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import payment.app.account_service.model.dto.AmountRequest;
import payment.app.account_service.model.dto.BalanceResponse;
import payment.app.account_service.model.dto.CreateAccountRequest;
import payment.app.account_service.model.dto.CreateAccountResponse;
import payment.app.account_service.service.AccountService;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request, @RequestHeader("X-User-Id") String userId) {

        System.out.println("Account Controller Hit");
        return accountService.createAccount(request, userId);
    }

    /*@GetMapping("/{accountNumber}/balance")
    public BalanceResponse getBalance(@PathVariable String accountNumber, @RequestHeader("X-User-Id") String userId) {
        return accountService.getBalance(accountNumber);
    }*/

    @PostMapping("/{accountNumber}/credit")
    //@ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<BalanceResponse> credit(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
       BalanceResponse response = accountService.credit(accountNumber, request);
       return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountNumber}/debit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void debit(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) {
        accountService.debit(accountNumber, request);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<?> getBalance(
            @PathVariable String accountNumber,
            @RequestHeader(value = "X-User-Id", required = false) String userId
            ) { //@RequestHeader("X-User-Role") String role

        BalanceResponse response = accountService.getBalance(accountNumber, userId);

        return ResponseEntity.ok(response);
    }
}
