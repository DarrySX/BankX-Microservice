package com.bankx.transactions.domain.exception;

public abstract class BusinessException extends RuntimeException {

    private final String code;

    protected BusinessException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
