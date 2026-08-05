package com.bankx.transactions.domain.exception;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException() {
        super("account_not_found");
    }
}
