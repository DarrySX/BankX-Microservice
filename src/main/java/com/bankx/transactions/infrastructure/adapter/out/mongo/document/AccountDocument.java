package com.bankx.transactions.infrastructure.adapter.out.mongo.document;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String number;

    private String holderName;
    private String currency;
    private BigDecimal balance;
}
