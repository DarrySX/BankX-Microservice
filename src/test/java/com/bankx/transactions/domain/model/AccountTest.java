package com.bankx.transactions.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bankx.transactions.domain.exception.InsufficientFundsException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final Account ACCOUNT =
            new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("2000"));

    @Test
    void debitSubtractsFromBalance() {
        var result = ACCOUNT.debit(new BigDecimal("500"));

        assertThat(result.balance()).isEqualByComparingTo("1500");
    }

    @Test
    void debitAllowsSpendingTheExactBalance() {
        var result = ACCOUNT.debit(new BigDecimal("2000"));

        assertThat(result.balance()).isEqualByComparingTo("0");
    }

    @Test
    void debitRejectsAmountAboveBalance() {
        var oneCentTooMuch = new BigDecimal("2000.01");

        assertThatThrownBy(() -> ACCOUNT.debit(oneCentTooMuch))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("insufficient_funds");
    }

    @Test
    void creditAddsToBalance() {
        var result = ACCOUNT.credit(new BigDecimal("250"));

        assertThat(result.balance()).isEqualByComparingTo("2250");
    }

    @Test
    void applyRoutesDebitAndCredit() {
        assertThat(ACCOUNT.apply(TransactionType.DEBIT, new BigDecimal("100")).balance())
                .isEqualByComparingTo("1900");
        assertThat(ACCOUNT.apply(TransactionType.CREDIT, new BigDecimal("100")).balance())
                .isEqualByComparingTo("2100");
    }

    @Test
    void balanceChangesKeepTheRestOfTheAccountIntact() {
        var result = ACCOUNT.credit(new BigDecimal("1"));

        assertThat(result.id()).isEqualTo("id-1");
        assertThat(result.number()).isEqualTo("001-0001");
        assertThat(result.holderName()).isEqualTo("Ana Peru");
        assertThat(result.currency()).isEqualTo("PEN");
    }
}
