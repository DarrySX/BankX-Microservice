package com.bankx.transactions.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankx.transactions.domain.exception.AccountNotFoundException;
import com.bankx.transactions.domain.model.Account;
import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.out.LoadAccountPort;
import com.bankx.transactions.domain.port.out.LoadTransactionsPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ListTransactionsServiceTest {

    private final LoadAccountPort loadAccount = mock(LoadAccountPort.class);
    private final LoadTransactionsPort loadTransactions = mock(LoadTransactionsPort.class);

    private final ListTransactionsService service = new ListTransactionsService(loadAccount, loadTransactions);

    @Test
    void listsTransactionsOfAnExistingAccount() {
        var account = new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("2000"));
        var first = Transaction.approved("id-1", TransactionType.DEBIT, new BigDecimal("50")).withId("tx-2");
        var second = Transaction.approved("id-1", TransactionType.DEBIT, new BigDecimal("100")).withId("tx-1");

        when(loadAccount.findByNumber("001-0001")).thenReturn(Mono.just(account));
        when(loadTransactions.findByAccountIdOrderByTimestampDesc("id-1"))
                .thenReturn(Flux.just(first, second));

        StepVerifier.create(service.byAccountNumber("001-0001"))
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();
    }

    @Test
    void emitsEmptyWhenTheAccountHasNoTransactions() {
        var account = new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("2000"));
        when(loadAccount.findByNumber("001-0001")).thenReturn(Mono.just(account));
        when(loadTransactions.findByAccountIdOrderByTimestampDesc("id-1")).thenReturn(Flux.empty());

        StepVerifier.create(service.byAccountNumber("001-0001"))
                .verifyComplete();
    }

    @Test
    void failsWhenTheAccountDoesNotExist() {
        when(loadAccount.findByNumber("999-9999")).thenReturn(Mono.empty());

        StepVerifier.create(service.byAccountNumber("999-9999"))
                .expectError(AccountNotFoundException.class)
                .verify();

        verify(loadTransactions, never()).findByAccountIdOrderByTimestampDesc(any());
    }
}
