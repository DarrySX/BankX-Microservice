package com.bankx.transactions.application.service;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.port.in.StreamTransactionsUseCase;
import com.bankx.transactions.domain.port.out.SubscribeTransactionEventsPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class StreamTransactionsService implements StreamTransactionsUseCase {

    private final SubscribeTransactionEventsPort subscribePort;

    @Override
    public Flux<Transaction> stream() {
        return subscribePort.events();
    }
}
