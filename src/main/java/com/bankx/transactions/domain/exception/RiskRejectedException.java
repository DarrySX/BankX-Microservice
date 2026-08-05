package com.bankx.transactions.domain.exception;

public class RiskRejectedException extends BusinessException {

    public RiskRejectedException() {
        super("risk_rejected");
    }
}
