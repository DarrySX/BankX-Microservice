package com.bankx.transactions.domain.port.out;

import com.bankx.transactions.domain.model.Transaction;
import reactor.core.publisher.Flux;

public interface LoadTransactionsPort {
    Flux<Transaction> findByAccountIdOrderByTimestampDesc(String accountId);
}
