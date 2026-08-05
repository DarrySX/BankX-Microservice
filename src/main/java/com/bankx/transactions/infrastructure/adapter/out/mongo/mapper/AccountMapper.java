package com.bankx.transactions.infrastructure.adapter.out.mongo.mapper;

import com.bankx.transactions.domain.model.Account;
import com.bankx.transactions.infrastructure.adapter.out.mongo.document.AccountDocument;

public final class AccountMapper {

    private AccountMapper() {}

    public static Account toDomain(AccountDocument d) {
        return new Account(d.getId(), d.getNumber(), d.getHolderName(), d.getCurrency(), d.getBalance());
    }

    public static AccountDocument toDocument(Account a) {
        return AccountDocument.builder()
                .id(a.id()).number(a.number()).holderName(a.holderName())
                .currency(a.currency()).balance(a.balance())
                .build();
    }
}
