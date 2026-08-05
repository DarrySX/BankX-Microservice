package com.bankx.transactions.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankx.transactions.domain.exception.AccountNotFoundException;
import com.bankx.transactions.domain.exception.InsufficientFundsException;
import com.bankx.transactions.domain.exception.RiskRejectedException;
import com.bankx.transactions.domain.model.Account;
import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.in.CreateTransactionCommand;
import com.bankx.transactions.domain.port.out.LoadAccountPort;
import com.bankx.transactions.domain.port.out.PublishTransactionEventPort;
import com.bankx.transactions.domain.port.out.RiskPolicyPort;
import com.bankx.transactions.domain.port.out.SaveAccountPort;
import com.bankx.transactions.domain.port.out.SaveTransactionPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Sin Mongo, sin H2 y sin contexto de Spring: el beneficio real de Clean Architecture. */
class CreateTransactionServiceTest {

    private final LoadAccountPort loadAccount = mock(LoadAccountPort.class);
    private final SaveAccountPort saveAccount = mock(SaveAccountPort.class);
    private final SaveTransactionPort saveTx = mock(SaveTransactionPort.class);
    private final RiskPolicyPort risk = mock(RiskPolicyPort.class);
    private final PublishTransactionEventPort events = mock(PublishTransactionEventPort.class);

    private final CreateTransactionService service =
            new CreateTransactionService(loadAccount, saveAccount, saveTx, risk, events);

    @Test
    void rejectsWhenAccountDoesNotExist() {
        when(loadAccount.findByNumber("999-9999")).thenReturn(Mono.empty());

        var cmd = new CreateTransactionCommand("999-9999", TransactionType.CREDIT, new BigDecimal("50"));

        StepVerifier.create(service.create(cmd))
                .expectError(AccountNotFoundException.class)
                .verify();

        verify(risk, never()).isAllowed(any(), any(), any());
    }

    @Test
    void rejectsWhenRiskPolicyDenies() {
        var account = new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("2000"));
        when(loadAccount.findByNumber("001-0001")).thenReturn(Mono.just(account));
        when(risk.isAllowed(any(), any(), any())).thenReturn(Mono.just(false));

        var cmd = new CreateTransactionCommand("001-0001", TransactionType.DEBIT, new BigDecimal("2000"));

        StepVerifier.create(service.create(cmd))
                .expectErrorMatches(e -> e instanceof RiskRejectedException)
                .verify();

        verify(saveTx, never()).save(any());
    }

    @Test
    void rejectsWhenBalanceIsNotEnough() {
        var account = new Account("id-2", "001-0002", "Luis Acuña", "PEN", new BigDecimal("800"));
        when(loadAccount.findByNumber("001-0002")).thenReturn(Mono.just(account));
        when(risk.isAllowed(any(), any(), any())).thenReturn(Mono.just(true));

        var cmd = new CreateTransactionCommand("001-0002", TransactionType.DEBIT, new BigDecimal("1200"));

        StepVerifier.create(service.create(cmd))
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    void debitsAccountAndPublishesEventOnSuccess() {
        var account = new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("2000"));
        when(loadAccount.findByNumber("001-0001")).thenReturn(Mono.just(account));
        when(risk.isAllowed(any(), any(), any())).thenReturn(Mono.just(true));
        when(saveAccount.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(saveTx.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            return Mono.just(t.withId("tx-1"));
        });

        var cmd = new CreateTransactionCommand("001-0001", TransactionType.DEBIT, new BigDecimal("100"));

        StepVerifier.create(service.create(cmd))
                .assertNext(tx -> {
                    org.assertj.core.api.Assertions.assertThat(tx.id()).isEqualTo("tx-1");
                    org.assertj.core.api.Assertions.assertThat(tx.accountId()).isEqualTo("id-1");
                })
                .verifyComplete();

        // el saldo debitado es el que se persiste: 2000 - 100
        verify(saveAccount).save(new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("1900")));
        // el evento SSE se emite sólo después de persistir
        verify(events).publish(any());
    }
}
