package payment.app.transaction_service.exception;

public class TransferProcessingException extends RuntimeException {

    public TransferProcessingException(String message) {
        super(message);
    }
}
