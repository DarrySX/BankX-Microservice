package com.bankx.transactions.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"DEBIT", "debit", "Debit", "  debit  "})
    void fromAcceptsAnyCasingAndSurroundingSpaces(String raw) {
        assertThat(TransactionType.from(raw)).isEqualTo(TransactionType.DEBIT);
    }

    @Test
    void fromParsesCredit() {
        assertThat(TransactionType.from("credit")).isEqualTo(TransactionType.CREDIT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRANSFER", "", "   "})
    void fromRejectsUnknownValuesWithTheDomainErrorCode(String raw) {
        assertThatThrownBy(() -> TransactionType.from(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid_transaction_type");
    }

    @Test
    void fromRejectsNullWithTheDomainErrorCode() {
        assertThatThrownBy(() -> TransactionType.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid_transaction_type");
    }

    @Test
    void enumsExposeTheirConstants() {
        assertThat(TransactionType.values()).containsExactly(TransactionType.DEBIT, TransactionType.CREDIT);
        assertThat(TransactionStatus.values()).containsExactly(TransactionStatus.OK, TransactionStatus.REJECTED);
    }
}
