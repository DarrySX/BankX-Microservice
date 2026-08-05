package com.bankx.transactions.domain.model;

public enum TransactionType {
    DEBIT, CREDIT;

    public static TransactionType from(String raw) {
        try {
            return TransactionType.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid_transaction_type");
        }
    }
}
