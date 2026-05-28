package payment.app.account_service.exception;

public class InsufficientBalanceException extends  RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
