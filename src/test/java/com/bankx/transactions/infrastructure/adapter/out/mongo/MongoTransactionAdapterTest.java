package com.bankx.transactions.infrastructure.adapter.out.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.infrastructure.adapter.out.mongo.document.TransactionDocument;
import com.bankx.transactions.infrastructure.adapter.out.mongo.repository.TransactionMongoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MongoTransactionAdapterTest {

    private static final Instant WHEN = Instant.parse("2026-08-05T23:05:00Z");

    private final TransactionMongoRepository repository = mock(TransactionMongoRepository.class);
    private final MongoTransactionAdapter adapter = new MongoTransactionAdapter(repository);

    private static TransactionDocument document(String id, String amount) {
        return TransactionDocument.builder()
                .id(id).accountId("acc-1").type("DEBIT").amount(new BigDecimal(amount))
                .timestamp(WHEN).status("OK").reason(null)
                .build();
    }

    @Test
    void saveAssignsTheGeneratedIdToTheDomainObject() {
        when(repository.save(any(TransactionDocument.class))).thenReturn(Mono.just(document("tx-1", "100")));

        var toSave = Transaction.approved("acc-1", TransactionType.DEBIT, new BigDecimal("100"));

        StepVerifier.create(adapter.save(toSave))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo("tx-1");
                    assertThat(saved.accountId()).isEqualTo("acc-1");
                })
                .verifyComplete();
    }

    @Test
    void listPreservesTheOrderComingFromTheRepository() {
        when(repository.findByAccountIdOrderByTimestampDesc("acc-1"))
                .thenReturn(Flux.just(document("tx-2", "50"), document("tx-1", "100")));

        StepVerifier.create(adapter.findByAccountIdOrderByTimestampDesc("acc-1"))
                .assertNext(tx -> assertThat(tx.id()).isEqualTo("tx-2"))
                .assertNext(tx -> assertThat(tx.id()).isEqualTo("tx-1"))
                .verifyComplete();
    }

    @Test
    void listIsEmptyWhenTheAccountHasNoTransactions() {
        when(repository.findByAccountIdOrderByTimestampDesc("acc-2")).thenReturn(Flux.empty());

        StepVerifier.create(adapter.findByAccountIdOrderByTimestampDesc("acc-2"))
                .verifyComplete();
    }
}
