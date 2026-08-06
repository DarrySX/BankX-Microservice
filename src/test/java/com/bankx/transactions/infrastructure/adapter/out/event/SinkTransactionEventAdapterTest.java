package com.bankx.transactions.infrastructure.adapter.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionType;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class SinkTransactionEventAdapterTest {

    private final SinkTransactionEventAdapter adapter = new SinkTransactionEventAdapter();

    private static Transaction tx(String id) {
        return Transaction.approved("acc-1", TransactionType.DEBIT, new BigDecimal("100")).withId(id);
    }

    @Test
    void publishedEventsReachTheSubscriber() {
        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(tx("tx-1")))
                .assertNext(received -> assertThat(received.id()).isEqualTo("tx-1"))
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void everySubscriberReceivesTheSameEvent() {
        var first = adapter.events();
        var second = adapter.events();

        StepVerifier.create(first)
                .then(() -> adapter.publish(tx("tx-1")))
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(5));

        StepVerifier.create(second)
                .then(() -> adapter.publish(tx("tx-2")))
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void aCancelledSubscriberDoesNotTerminateTheSinkForTheNextOne() {
        StepVerifier.create(adapter.events())
                .thenCancel()
                .verify(Duration.ofSeconds(5));

        // autoCancel=false: el sink sigue vivo para quien se suscriba después
        StepVerifier.create(adapter.events())
                .then(() -> adapter.publish(tx("tx-3")))
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }
}
