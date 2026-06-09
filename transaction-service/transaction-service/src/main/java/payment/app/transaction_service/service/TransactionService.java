package payment.app.transaction_service.service;

import payment.app.transaction_service.model.dto.TransactionResponse;
import payment.app.transaction_service.model.dto.TransferRequest;
import payment.app.transaction_service.model.entity.Transaction;

import java.util.List;

public interface TransactionService {

   TransactionResponse transfer(TransferRequest request, String token, String idempotencyKey);
   List<TransactionResponse> getTransactions(String accountNumber);
   List<Transaction> getAllTransactions();
   TransactionResponse reverseTransaction(String transactionId);
}
