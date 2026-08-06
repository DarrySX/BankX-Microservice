package com.bankx.transactions.application.service;

import com.bankx.transactions.domain.exception.AccountNotFoundException;
import com.bankx.transactions.domain.exception.RiskRejectedException;
import com.bankx.transactions.domain.model.Account;
import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.port.in.CreateTransactionCommand;
import com.bankx.transactions.domain.port.in.CreateTransactionUseCase;
import com.bankx.transactions.domain.port.out.LoadAccountPort;
import com.bankx.transactions.domain.port.out.PublishTransactionEventPort;
import com.bankx.transactions.domain.port.out.RiskPolicyPort;
import com.bankx.transactions.domain.port.out.SaveAccountPort;
import com.bankx.transactions.domain.port.out.SaveTransactionPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
public class CreateTransactionService implements CreateTransactionUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final SaveTransactionPort saveTransactionPort;
    private final RiskPolicyPort riskPolicyPort;
    private final PublishTransactionEventPort eventPort;

    @Override
    public Mono<Transaction> create(CreateTransactionCommand cmd) {
        return loadAccountPort.findByNumber(cmd.accountNumber())
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMap(account -> evaluateRisk(account, cmd))
                .flatMap(account -> applyAndPersist(account, cmd))
                .doOnNext(eventPort::publish);   // notificación SSE
    }

    private Mono<Account> evaluateRisk(Account account, CreateTransactionCommand cmd) {
        return riskPolicyPort.isAllowed(account.currency(), cmd.type(), cmd.amount())
                .flatMap(allowed -> Boolean.TRUE.equals(allowed)
                        ? Mono.just(account)
                        : Mono.error(new RiskRejectedException()));
    }

    private Mono<Transaction> applyAndPersist(Account account, CreateTransactionCommand cmd) {
        return Mono.fromCallable(() -> account.apply(cmd.type(), cmd.amount()))  // puede lanzar InsufficientFunds
                .publishOn(Schedulers.parallel())
                .flatMap(saveAccountPort::save)
                .flatMap(saved -> saveTransactionPort.save(
                        Transaction.approved(saved.id(), cmd.type(), cmd.amount())));
    }
}
