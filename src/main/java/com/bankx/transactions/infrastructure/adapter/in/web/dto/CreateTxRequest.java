package com.bankx.transactions.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateTxRequest {

    @NotBlank
    private String accountNumber;

    @NotBlank
    @Pattern(regexp = "(?i)DEBIT|CREDIT", message = "invalid_transaction_type")
    private String type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
}
