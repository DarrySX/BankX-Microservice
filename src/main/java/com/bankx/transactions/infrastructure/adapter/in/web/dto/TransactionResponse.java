package com.bankx.transactions.infrastructure.adapter.in.web.dto;

import com.bankx.transactions.domain.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String id, String accountId, String type, BigDecimal amount,
        Instant timestamp, String status, String reason
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.id(), t.accountId(), t.type().name(),
                t.amount(), t.timestamp(), t.status().name(), t.reason());
    }
}
