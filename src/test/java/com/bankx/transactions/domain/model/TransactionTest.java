package com.bankx.transactions.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionTest {

    @Test
    void approvedHasNoIdAndNoReason() {
        var tx = Transaction.approved("acc-1", TransactionType.DEBIT, new BigDecimal("100"));

        assertThat(tx.id()).isNull();
        assertThat(tx.accountId()).isEqualTo("acc-1");
        assertThat(tx.type()).isEqualTo(TransactionType.DEBIT);
        assertThat(tx.amount()).isEqualByComparingTo("100");
        assertThat(tx.status()).isEqualTo(TransactionStatus.OK);
        assertThat(tx.reason()).isNull();
        assertThat(tx.timestamp()).isNotNull();
    }

    @Test
    void rejectedCarriesTheReason() {
        var tx = Transaction.rejected("acc-1", TransactionType.DEBIT, new BigDecimal("9000"), "risk_rejected");

        assertThat(tx.id()).isNull();
        assertThat(tx.status()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(tx.reason()).isEqualTo("risk_rejected");
    }

    @Test
    void withIdOnlyReplacesTheId() {
        var original = Transaction.approved("acc-1", TransactionType.CREDIT, new BigDecimal("50"));

        var stored = original.withId("tx-1");

        assertThat(stored.id()).isEqualTo("tx-1");
        assertThat(stored.accountId()).isEqualTo(original.accountId());
        assertThat(stored.type()).isEqualTo(original.type());
        assertThat(stored.amount()).isEqualByComparingTo(original.amount());
        assertThat(stored.timestamp()).isEqualTo(original.timestamp());
        assertThat(stored.status()).isEqualTo(original.status());
        assertThat(stored.reason()).isEqualTo(original.reason());
    }
}
