package com.bankx.transactions.domain.port.out;

import com.bankx.transactions.domain.model.TransactionType;
import java.math.BigDecimal;
import reactor.core.publisher.Mono;

public interface RiskPolicyPort {
    Mono<Boolean> isAllowed(String currency, TransactionType type, BigDecimal amount);
}
