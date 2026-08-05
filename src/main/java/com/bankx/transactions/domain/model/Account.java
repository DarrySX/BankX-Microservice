package com.bankx.transactions.domain.model;

import com.bankx.transactions.domain.exception.InsufficientFundsException;
import java.math.BigDecimal;

public record Account(
        String id,
        String number,
        String holderName,
        String currency,
        BigDecimal balance
) {
    public Account apply(TransactionType type, BigDecimal amount) {
        return type == TransactionType.DEBIT ? debit(amount) : credit(amount);
    }

    public Account debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        return withBalance(balance.subtract(amount));
    }

    public Account credit(BigDecimal amount) {
        return withBalance(balance.add(amount));
    }

    private Account withBalance(BigDecimal newBalance) {
        return new Account(id, number, holderName, currency, newBalance);
    }
}
