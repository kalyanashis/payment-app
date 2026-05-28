package payment.app.account_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.app.account_service.exception.InsufficientBalanceException;
import payment.app.account_service.model.dto.AmountRequest;
import payment.app.account_service.model.dto.BalanceResponse;
import payment.app.account_service.model.dto.CreateAccountRequest;
import payment.app.account_service.model.dto.CreateAccountResponse;
import payment.app.account_service.model.entity.Account;
import payment.app.account_service.repository.AccountRepository;
import payment.app.account_service.service.AccountService;
import payment.app.account_service.util.AccountNumberGenerator;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public CreateAccountResponse createAccount(CreateAccountRequest request, String userId) {
        String accountNumber = generateAccountNumber();
        Account account = new Account(accountNumber, request.getCustomerName(), request.getInitialBalance(), userId);
        Account savedAccount = accountRepository.save(account);
        return new CreateAccountResponse(savedAccount.getAccountNumber(), savedAccount.getBalance(), savedAccount.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber, String userId) {
        Account account = getAccount(accountNumber);
        if(userId != null && !account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }
        return new BalanceResponse(account.getAccountNumber(), account.getBalance(), "Balance fetched successfully");
    }

    @Override
    public BalanceResponse credit(String accountNumber, AmountRequest request) {
        Account account = getAccount(accountNumber);
        account.credit(request.getAmount());
        accountRepository.save(account);

        return new BalanceResponse(account.getAccountNumber(), account.getBalance(), "Amount credited successfully");
    }

    @Override
    public void debit(String accountNumber, AmountRequest request) {
        Account account = getAccount(accountNumber);
        if(account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        account.debit(request.getAmount());
        accountRepository.save(account);
    }

    // -------- Helper methods --------

    private Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
    }

    private String generateAccountNumber() {
       return AccountNumberGenerator.generate();
    }
}
