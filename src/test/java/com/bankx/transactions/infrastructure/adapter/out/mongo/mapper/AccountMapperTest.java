package com.bankx.transactions.infrastructure.adapter.out.mongo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankx.transactions.domain.model.Account;
import com.bankx.transactions.infrastructure.adapter.out.mongo.document.AccountDocument;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountMapperTest {

    @Test
    void mapsDocumentToDomain() {
        var document = AccountDocument.builder()
                .id("id-1").number("001-0001").holderName("Ana Peru")
                .currency("PEN").balance(new BigDecimal("2000"))
                .build();

        var account = AccountMapper.toDomain(document);

        assertThat(account.id()).isEqualTo("id-1");
        assertThat(account.number()).isEqualTo("001-0001");
        assertThat(account.holderName()).isEqualTo("Ana Peru");
        assertThat(account.currency()).isEqualTo("PEN");
        assertThat(account.balance()).isEqualByComparingTo("2000");
    }

    @Test
    void mapsDomainToDocument() {
        var account = new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("2000"));

        var document = AccountMapper.toDocument(account);

        assertThat(document.getId()).isEqualTo("id-1");
        assertThat(document.getNumber()).isEqualTo("001-0001");
        assertThat(document.getHolderName()).isEqualTo("Ana Peru");
        assertThat(document.getCurrency()).isEqualTo("PEN");
        assertThat(document.getBalance()).isEqualByComparingTo("2000");
    }

    @Test
    void roundTripKeepsEveryField() {
        var original = new Account("id-9", "001-0009", "Luis Acuña", "USD", new BigDecimal("12.34"));

        assertThat(AccountMapper.toDomain(AccountMapper.toDocument(original))).isEqualTo(original);
    }

    @Test
    void keepsTheNullIdOfAnAccountThatWasNeverPersisted() {
        var newAccount = new Account(null, "001-0010", "Nuevo", "PEN", BigDecimal.ZERO);

        assertThat(AccountMapper.toDocument(newAccount).getId()).isNull();
    }
}
