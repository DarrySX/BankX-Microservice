package com.bankx.transactions.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String id,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        Instant timestamp,
        TransactionStatus status,
        String reason
) {
    public static Transaction approved(String accountId, TransactionType type, BigDecimal amount) {
        return new Transaction(null, accountId, type, amount, Instant.now(), TransactionStatus.OK, null);
    }

    public static Transaction rejected(String accountId, TransactionType type, BigDecimal amount, String reason) {
        return new Transaction(null, accountId, type, amount, Instant.now(), TransactionStatus.REJECTED, reason);
    }

    public Transaction withId(String id) {
        return new Transaction(id, accountId, type, amount, timestamp, status, reason);
    }
}
