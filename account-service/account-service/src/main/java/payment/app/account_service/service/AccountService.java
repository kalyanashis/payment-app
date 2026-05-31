package payment.app.account_service.service;

import payment.app.account_service.model.dto.AmountRequest;
import payment.app.account_service.model.dto.BalanceResponse;
import payment.app.account_service.model.dto.CreateAccountRequest;
import payment.app.account_service.model.dto.CreateAccountResponse;

public interface AccountService {

    CreateAccountResponse createAccount(CreateAccountRequest request, String userId);

    BalanceResponse getBalance(String accountNumber, String userId);

    BalanceResponse credit(String accountNumber, AmountRequest request);

    BalanceResponse debit(String accountNumber, AmountRequest request);
}
