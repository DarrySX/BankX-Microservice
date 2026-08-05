package com.bankx.transactions.domain.port.out;

import com.bankx.transactions.domain.model.Transaction;
import reactor.core.publisher.Mono;

public interface SaveTransactionPort {
    Mono<Transaction> save(Transaction transaction);
}
