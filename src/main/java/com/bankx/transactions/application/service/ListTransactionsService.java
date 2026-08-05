package com.bankx.transactions.application.service;

import com.bankx.transactions.domain.exception.AccountNotFoundException;
import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.port.in.ListTransactionsUseCase;
import com.bankx.transactions.domain.port.out.LoadAccountPort;
import com.bankx.transactions.domain.port.out.LoadTransactionsPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ListTransactionsService implements ListTransactionsUseCase {

    private final LoadAccountPort loadAccountPort;
    private final LoadTransactionsPort loadTransactionsPort;

    @Override
    public Flux<Transaction> byAccountNumber(String accountNumber) {
        return loadAccountPort.findByNumber(accountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMapMany(acc -> loadTransactionsPort.findByAccountIdOrderByTimestampDesc(acc.id()));
    }
}
