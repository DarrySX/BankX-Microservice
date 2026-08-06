package com.bankx.transactions.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankx.transactions.domain.exception.AccountNotFoundException;
import com.bankx.transactions.domain.exception.InsufficientFundsException;
import com.bankx.transactions.domain.exception.RiskRejectedException;
import com.bankx.transactions.infrastructure.adapter.in.web.dto.CreateTxRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

class GlobalErrorHandlerTest {

    private final GlobalErrorHandler handler = new GlobalErrorHandler();

    @Test
    void businessExceptionsBecomeBadRequestWithTheirDomainCode() {
        assertThat(handler.handleBusiness(new AccountNotFoundException()).getBody())
                .containsEntry("error", "account_not_found");
        assertThat(handler.handleBusiness(new RiskRejectedException()).getBody())
                .containsEntry("error", "risk_rejected");

        var response = handler.handleBusiness(new InsufficientFundsException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "insufficient_funds");
    }

    @Test
    void validationErrorsAreReportedFieldByField() throws NoSuchMethodException {
        var target = new CreateTxRequest();
        var binding = new BeanPropertyBindingResult(target, "createTxRequest");
        binding.rejectValue("accountNumber", "NotBlank", "no debe estar vacío");
        binding.rejectValue("type", "Pattern", "invalid_transaction_type");
        var parameter = new MethodParameter(
                TransactionController.class.getDeclaredMethod("create", CreateTxRequest.class), 0);

        var response = handler.handleValidation(new WebExchangeBindException(parameter, binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "validation_error");
        @SuppressWarnings("unchecked")
        var details = (Map<String, String>) response.getBody().get("details");
        assertThat(details)
                .containsEntry("accountNumber", "no debe estar vacío")
                .containsEntry("type", "invalid_transaction_type");
    }

    @Test
    void illegalArgumentsSurfaceTheirOwnMessageAsCode() {
        var response = handler.handleIllegal(new IllegalArgumentException("invalid_transaction_type"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_transaction_type");
    }

    @Test
    void exceptionsThatCarryTheirOwnStatusKeepIt() {
        var withReason = handler.handleStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No static resource"));

        assertThat(withReason.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(withReason.getBody()).containsEntry("error", "No static resource");
    }

    @Test
    void statusExceptionsWithoutReasonFallBackToTheStatusPhrase() {
        var withoutReason = handler.handleStatus(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE));

        assertThat(withoutReason.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(withoutReason.getBody()).containsEntry("error", "Unsupported Media Type");
    }

    @Test
    void anythingElseBecomesAnOpaqueInternalError() {
        var response = handler.handleGeneric(new IllegalStateException("detalle que no debe filtrarse"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "internal_error");
    }
}
