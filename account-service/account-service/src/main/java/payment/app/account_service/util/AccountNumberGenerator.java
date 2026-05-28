package payment.app.account_service.util;

import java.util.UUID;

public final class AccountNumberGenerator {

    private AccountNumberGenerator() {
        // utility class
    }

    public static String generate() {
        return UUID.randomUUID()
                .toString()
                .replace("-","")
                .substring(0, 10)
                .toUpperCase();
    }
}
