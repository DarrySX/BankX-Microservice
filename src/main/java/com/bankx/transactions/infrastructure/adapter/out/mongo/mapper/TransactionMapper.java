package com.bankx.transactions.infrastructure.adapter.out.mongo.mapper;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionStatus;
import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.infrastructure.adapter.out.mongo.document.TransactionDocument;

public final class TransactionMapper {

    private TransactionMapper() {}

    public static Transaction toDomain(TransactionDocument d) {
        return new Transaction(d.getId(), d.getAccountId(),
                TransactionType.valueOf(d.getType()), d.getAmount(), d.getTimestamp(),
                TransactionStatus.valueOf(d.getStatus()), d.getReason());
    }

    public static TransactionDocument toDocument(Transaction t) {
        return TransactionDocument.builder()
                .id(t.id()).accountId(t.accountId()).type(t.type().name())
                .amount(t.amount()).timestamp(t.timestamp())
                .status(t.status().name()).reason(t.reason())
                .build();
    }
}
