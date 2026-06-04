package payment.app.transaction_service.exception;

public class DailyTransferLimitExceededException extends RuntimeException {

    public DailyTransferLimitExceededException(String message) {
        super(message);
    }
}
