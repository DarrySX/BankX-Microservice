package com.bankx.transactions.infrastructure.adapter.out.mongo.document;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDocument {

    @Id
    private String id;

    private String accountId;
    private String type;
    private BigDecimal amount;
    private Instant timestamp;
    private String status;
    private String reason;
}
