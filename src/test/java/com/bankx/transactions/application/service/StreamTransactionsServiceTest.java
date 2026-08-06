package com.bankx.transactions.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.out.SubscribeTransactionEventsPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class StreamTransactionsServiceTest {

    private final SubscribeTransactionEventsPort subscribePort = mock(SubscribeTransactionEventsPort.class);
    private final StreamTransactionsService service = new StreamTransactionsService(subscribePort);

    @Test
    void relaysEveryEventFromTheSubscriptionPort() {
        var tx = Transaction.approved("id-1", TransactionType.DEBIT, new BigDecimal("100")).withId("tx-1");
        when(subscribePort.events()).thenReturn(Flux.just(tx));

        StepVerifier.create(service.stream())
                .expectNext(tx)
                .verifyComplete();
    }

    @Test
    void propagatesErrorsFromTheSubscriptionPort() {
        when(subscribePort.events()).thenReturn(Flux.error(new IllegalStateException("sink_down")));

        StepVerifier.create(service.stream())
                .expectError(IllegalStateException.class)
                .verify();
    }
}
