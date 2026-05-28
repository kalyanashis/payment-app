package payment.app.transaction_service.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.app.transaction_service.client.AccountServiceClient;
import payment.app.transaction_service.exception.TransactionFailedException;
import payment.app.transaction_service.exception.TransferProcessingException;
import payment.app.transaction_service.kafka.event.TransactionCompletedEvent;
import payment.app.transaction_service.kafka.producer.TransactionEventProducer;
import payment.app.transaction_service.model.dto.*;
import payment.app.transaction_service.model.entity.Transaction;
import payment.app.transaction_service.repository.TransactionRepository;
import payment.app.transaction_service.service.TransactionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final TransactionEventProducer eventProducer;

    @Override
    @CacheEvict(value = "transactions", key = "#request.fromAccount")
    public TransactionResponse transfer(TransferRequest request, String token, String idempotencyKey) {

        String transactionId = generateTransactionId();
        boolean debitDone = false;

        //idempotency check
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get(), true);
        }

        try {
            //Check balance
            BalanceResponse balance = accountServiceClient.getBalance(request.getFromAccount(), token);
            if(balance.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            // Debit from source account
            accountServiceClient.debit(request.getFromAccount(), new AmountRequest(request.getAmount()), token);
            debitDone = true;

            /*if (true) {
                throw new TransferProcessingException("Simulated credit failure");
            }*/

            // Credit to destination account
            accountServiceClient.credit(request.getToAccount(), new AmountRequest(request.getAmount()), token);

            // Save successful transaction
            Transaction transaction = new Transaction(transactionId,
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    TransactionType.TRANSFER,
                    "SUCCESS");

            transaction.assignIdempotencyKey(idempotencyKey);

            log.info("Saving transaction: {}", transaction.getTransactionId());
            transactionRepository.save(transaction);
            log.info("Transaction saved successfully");

            TransactionCompletedEvent event =
                   new TransactionCompletedEvent(
                        transaction.getTransactionId(),
                        transaction.getFromAccount(),
                        transaction.getToAccount(),
                        transaction.getAmount(),
                        transaction.getStatus()
                   );
            eventProducer.publish(event);

            return toResponse(transaction, false);
        } catch(FeignException | TransferProcessingException ex) {

            // Compensation
            if(debitDone) {
                try {
                    accountServiceClient.credit(request.getFromAccount(), new AmountRequest(request.getAmount()), token);
                    log.info("Compensation rollback successful");
                } catch(Exception exc) {
                    log.info("Rollback failed: " + exc.getMessage());
                }
            }
            // Persist failed transaction
            Transaction failedTransaction = new Transaction(
                    transactionId,
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    TransactionType.TRANSFER,
                    "FAILED");

            transactionRepository.save(failedTransaction);
            throw new TransactionFailedException(
                    "Transaction failed: " + ex.getMessage()
            );
        }

    }

    @Override
    @Cacheable(value = "transactions", key = "#accountNumber")
    public List<TransactionResponse> getTransactions(String accountNumber) {

        System.out.println("FETCHING FROM DB...");

        return transactionRepository.findByFromAccountOrToAccount(accountNumber, accountNumber)
                .stream()
                .map(transaction -> toResponse(transaction, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // ---------- Helper methods ----------

    private TransactionResponse toResponse(Transaction transaction, boolean replay) {

        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getFromAccount(),
                transaction.getToAccount(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                replay);
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}
