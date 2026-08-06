package com.bankx.transactions.infrastructure.adapter.out.mongo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionStatus;
import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.infrastructure.adapter.out.mongo.document.TransactionDocument;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransactionMapperTest {

    private static final Instant WHEN = Instant.parse("2026-08-05T23:05:00Z");

    @Test
    void mapsDocumentToDomain() {
        var document = TransactionDocument.builder()
                .id("tx-1").accountId("acc-1").type("DEBIT")
                .amount(new BigDecimal("100")).timestamp(WHEN)
                .status("OK").reason(null)
                .build();

        var transaction = TransactionMapper.toDomain(document);

        assertThat(transaction.id()).isEqualTo("tx-1");
        assertThat(transaction.accountId()).isEqualTo("acc-1");
        assertThat(transaction.type()).isEqualTo(TransactionType.DEBIT);
        assertThat(transaction.amount()).isEqualByComparingTo("100");
        assertThat(transaction.timestamp()).isEqualTo(WHEN);
        assertThat(transaction.status()).isEqualTo(TransactionStatus.OK);
        assertThat(transaction.reason()).isNull();
    }

    @Test
    void mapsDomainToDocumentAsStrings() {
        var transaction = new Transaction("tx-1", "acc-1", TransactionType.CREDIT,
                new BigDecimal("50"), WHEN, TransactionStatus.REJECTED, "risk_rejected");

        var document = TransactionMapper.toDocument(transaction);

        assertThat(document.getType()).isEqualTo("CREDIT");
        assertThat(document.getStatus()).isEqualTo("REJECTED");
        assertThat(document.getReason()).isEqualTo("risk_rejected");
        assertThat(document.getTimestamp()).isEqualTo(WHEN);
    }

    @Test
    void roundTripKeepsEveryField() {
        var original = new Transaction("tx-7", "acc-7", TransactionType.DEBIT,
                new BigDecimal("1.05"), WHEN, TransactionStatus.OK, null);

        assertThat(TransactionMapper.toDomain(TransactionMapper.toDocument(original))).isEqualTo(original);
    }
}
