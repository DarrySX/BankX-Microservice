package com.bankx.transactions.domain.port.out;

import com.bankx.transactions.domain.model.Transaction;

public interface PublishTransactionEventPort {
    void publish(Transaction transaction);
}
