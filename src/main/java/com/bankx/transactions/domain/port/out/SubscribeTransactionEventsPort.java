package com.bankx.transactions.domain.port.out;

import com.bankx.transactions.domain.model.Transaction;
import reactor.core.publisher.Flux;

public interface SubscribeTransactionEventsPort {
    Flux<Transaction> events();
}
