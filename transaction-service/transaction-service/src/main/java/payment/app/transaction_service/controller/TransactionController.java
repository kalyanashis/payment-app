package payment.app.transaction_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import payment.app.transaction_service.model.dto.TransactionResponse;
import payment.app.transaction_service.model.dto.TransferRequest;
import payment.app.transaction_service.service.TransactionService;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    //@ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                        @RequestHeader("Authorization") String token,
                                                        @RequestHeader("Idempotency-Key") String idempotencyKey) {
        TransactionResponse response = transactionService.transfer(request, token, idempotencyKey);

        if(response.isReplay()) {
            return ResponseEntity.ok(response);
        }
        return  ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reverse/{transactionId}")
    ResponseEntity<TransactionResponse> reverseTransaction(@PathVariable String transactionId) {
        TransactionResponse response = transactionService.reverseTransaction(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}")
    public List<TransactionResponse> getTransactions(
            @PathVariable String accountNumber) {
        return transactionService.getTransactions(accountNumber);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllTransactions(HttpServletRequest request) {

        String role = request.getHeader("X-User-Role");

        if(!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/export/csv/{accountNumber}")
    public ResponseEntity<byte[]> exportCSV(@PathVariable String accountNumber) {

        String fileName = "Account-Report_" + accountNumber + ".csv";

        byte[] csvData = transactionService.exportStatementCSV(accountNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/export/pdf/{accountNumber}")
    public ResponseEntity<byte[]> exportPDF(@PathVariable String accountNumber) {

        String fileName = "Account-Report_" + accountNumber + ".pdf";

        byte[] pdfData = transactionService.exportStatementPDF(accountNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}
