package payment.app.transaction_service.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.text.Document;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.app.transaction_service.client.AccountServiceClient;
import payment.app.transaction_service.exception.*;
import payment.app.transaction_service.kafka.event.TransactionCompletedEvent;
import payment.app.transaction_service.kafka.event.TransactionReversedEvent;
import payment.app.transaction_service.kafka.producer.TransactionEventProducer;
import payment.app.transaction_service.model.dto.*;
import payment.app.transaction_service.model.entity.Transaction;
import payment.app.transaction_service.repository.TransactionRepository;
import payment.app.transaction_service.service.TransactionService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private static final DateTimeFormatter STATEMENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy | hh:mm a", Locale.ENGLISH);

    @Value("${transfer.daily-limit}")
    private BigDecimal dailyTransferLimit;

    @Value(("${payment.reversal.allowed-hours}"))
    private long reversalAllowedHours;

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
            //Check daily transfer limit
            validateDailyLimit(request.getFromAccount(), request.getAmount());

            //Check balance
            BalanceResponse balance = accountServiceClient.getBalance(request.getFromAccount());
            if(balance.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }

            // Debit from source account
            accountServiceClient.debit(request.getFromAccount(), new AmountRequest(request.getAmount()));
            debitDone = true;

            /*if (true) {
                throw new TransferProcessingException("Simulated credit failure");
            }*/

            // Credit to destination account
            accountServiceClient.credit(request.getToAccount(), new AmountRequest(request.getAmount()));

            // Save successful transaction
            Transaction transaction = new Transaction(transactionId,
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    TransactionType.TRANSFER,
                    SUCCESS);

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

            // Compensation logic
            if(debitDone) {
                try {
                    accountServiceClient.credit(request.getFromAccount(), new AmountRequest(request.getAmount()));
                    log.info("Compensation rollback successful");
                } catch(Exception exc) {
                    log.info("Rollback failed: {}", exc.getMessage());
                }
            }
            // Persist failed transaction
            Transaction failedTransaction = new Transaction(
                    transactionId,
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    TransactionType.TRANSFER,
                    FAILED);

            transactionRepository.save(failedTransaction);
            throw new TransactionFailedException(
                    "Transaction failed: " + ex.getMessage()
            );
        }

    }

    @Override
    @Cacheable(value = "transactions", key = "#accountNumber")
    public List<TransactionResponse> getTransactions(String accountNumber) {

        log.info("FETCHING FROM DB...");

        return transactionRepository.findByFromAccountOrToAccount(accountNumber, accountNumber)
                .stream()
                .map(transaction -> toResponse(transaction, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    public TransactionResponse reverseTransaction(String transactionId) {

        // Retrieve the original transaction using business transaction ID
        Transaction original = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        // Only transfer type transactions are eligible for reversal
        if(original.getType() != TransactionType.TRANSFER) {
            throw new IllegalArgumentException("Only transfer type transactions can be reversed");
        }

        // Only successfully completed transfers can be reversed
        if(!SUCCESS.equals(original.getStatus())) {
            throw new IllegalArgumentException("Only successful transactions can be reversed");
        }

        // Validate reversal window
        validateReversalWindow(original);

        // Prevent duplicate reversal of the same transaction
        if(transactionRepository.existsByOriginalTransactionId(transactionId)) {
            throw new TransactionAlreadyReversedException("Transaction already reversed");
        }

        // Verify that the recipient still has sufficient balance to return the transferred amount
        BalanceResponse balance = accountServiceClient.getBalance(original.getToAccount());

        if(balance.getBalance().compareTo(original.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for reversal");
        }

        // Generate a new transaction ID for the reversal transaction
        String reversalTransactionId = generateTransactionId();

        boolean debitDone = false;

        try {
            // Reverse Step 1:
            // Debit the original recipient account
            accountServiceClient.debit(original.getToAccount(), new AmountRequest(original.getAmount()));

            debitDone = true;

            // Reverse Step 2:
            // Credit the original sender account
            accountServiceClient.credit(original.getFromAccount(), new AmountRequest(original.getAmount()));

            // Create reversal transaction record for audit/history
            Transaction reversal = new Transaction(reversalTransactionId,
                    original.getToAccount(),
                    original.getFromAccount(),
                    original.getAmount(),
                    TransactionType.REVERSAL,
                    SUCCESS);

            // Link reversal transaction to original transaction
            reversal.assignOriginalTransactionId(original.getTransactionId());

            transactionRepository.save(reversal);

            TransactionReversedEvent event =
                    new TransactionReversedEvent(
                            reversal.getTransactionId(),
                            reversal.getOriginalTransactionId(),
                            reversal.getFromAccount(),
                            reversal.getToAccount(),
                            reversal.getAmount(),
                            reversal.getStatus()
                    );
            eventProducer.publishReversal(event);

            // Return API response
            return toResponse(reversal, false);

        } catch(FeignException | TransferProcessingException ex) {

            // Compensation Logic:
            // If recipient was already debited but sender could not be credited,
            // restore the amount back to recipient account
            if(debitDone) {
                try {
                    accountServiceClient.credit(original.getToAccount(), new AmountRequest(original.getAmount()));
                    log.info("Reversal rollback successful");
                } catch(Exception exc) {
                    log.error("Reversal rollback failed: {}", exc.getMessage());
                }
            }

            // Persist failed reversal attempt for audit purposes
            Transaction failedReversal = new Transaction(
                    reversalTransactionId,
                    original.getToAccount(),
                    original.getFromAccount(),
                    original.getAmount(),
                    TransactionType.REVERSAL,
                    FAILED
            );

            failedReversal.assignOriginalTransactionId(original.getTransactionId());
            transactionRepository.save(failedReversal);
            log.info(
                    "Reversal transaction saved successfully. Original Transaction: {}, Reversal Transaction: {}",
                    original.getTransactionId(),
                    reversalTransactionId
            );

            throw new TransactionFailedException("Reversal failed: " + ex.getMessage());
        }
    }

    @Override
    public byte[] exportStatementCSV(String accountNumber) {

        List<Transaction> transactions = transactionRepository.getStatement(accountNumber);
        StringBuilder csvBuilder = new StringBuilder();

        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions found for account: " + accountNumber);
        }

        csvBuilder.append("TransactionId,Type,FromAccount,ToAccount,Amount,Status,CreatedAt\n");

        for(Transaction transaction : transactions) {
            csvBuilder.append(transaction.getTransactionId())
                    .append(",")
                    .append(transaction.getType())
                    .append(",")
                    .append(transaction.getFromAccount())
                    .append(",")
                    .append(transaction.getToAccount())
                    .append(",")
                    .append(transaction.getAmount())
                    .append(",")
                    .append(transaction.getStatus())
                    .append(",")
                    .append(transaction.getCreatedAt().format(STATEMENT_DATE_FORMATTER))
                    .append("\n");
        }

        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportStatementPDF(String accountNumber) {

        List<Transaction> transactions = transactionRepository.getStatement(accountNumber);

        if(transactions.isEmpty()) {
            throw new TransactionNotFoundException("No transactions found for account: " + accountNumber);
        }

        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();

            PdfWriter.getInstance(document, outputStream);

            document.open();

            document.add(new Paragraph("Account Statement"));
            document.add(new Paragraph("Account Number: " + accountNumber));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);

            table.addCell("Transaction ID");
            table.addCell("Type");
            table.addCell("Amount");
            table.addCell("Status");
            table.addCell("Created At");

            for(Transaction transaction : transactions) {
                table.addCell(transaction.getTransactionId());
                table.addCell(transaction.getType().name());
                table.addCell(transaction.getAmount().toString());
                table.addCell(transaction.getStatus());
                table.addCell(transaction.getCreatedAt().format(STATEMENT_DATE_FORMATTER));
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();

        } catch(Exception ex) {
            throw new RuntimeException("Failed to generate PDF statement", ex);
        }
    }

    private void validateDailyLimit(String accountNumber, BigDecimal transferAmount) {

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        BigDecimal transferredToday = transactionRepository.getTodayTransferAmount(accountNumber, TransactionType.TRANSFER, startOfDay);
        BigDecimal totalAmount = transferredToday.add(transferAmount);

        if(totalAmount.compareTo(dailyTransferLimit) > 0) {

            throw new DailyTransferLimitExceededException(
                    String.format(
                            "Daily transfer limit exceeded. Limit=%s, Used=%s, Requested=%s",
                            dailyTransferLimit,
                            transferredToday,
                            transferAmount
                    )
            );
        }
    }

    private void validateReversalWindow(Transaction transaction) {

        LocalDateTime cutOff = LocalDateTime.now().minusHours(reversalAllowedHours);
        if(transaction.getCreatedAt().isBefore(cutOff)) {
            throw new InvalidTransactionException("Reversal period has expired");
        }
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

        String random =  UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();

        return "TXN-" + random;
    }
}
