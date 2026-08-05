package com.bankx.transactions.domain.port.in;

import com.bankx.transactions.domain.model.Transaction;
import reactor.core.publisher.Flux;

public interface StreamTransactionsUseCase {
    Flux<Transaction> stream();
}
