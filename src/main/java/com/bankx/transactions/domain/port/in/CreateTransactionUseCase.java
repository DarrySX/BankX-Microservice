package com.bankx.transactions.domain.port.in;

import com.bankx.transactions.domain.model.Transaction;
import reactor.core.publisher.Mono;

public interface CreateTransactionUseCase {
    Mono<Transaction> create(CreateTransactionCommand command);
}
