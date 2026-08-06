package com.bankx.transactions.domain.port.in;

import com.bankx.transactions.domain.model.TransactionType;
import java.math.BigDecimal;

public record CreateTransactionCommand(String accountNumber, TransactionType type, BigDecimal amount) { }
