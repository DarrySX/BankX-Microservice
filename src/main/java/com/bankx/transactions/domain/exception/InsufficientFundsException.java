package com.bankx.transactions.domain.exception;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException() {
        super("insufficient_funds");
    }
}
