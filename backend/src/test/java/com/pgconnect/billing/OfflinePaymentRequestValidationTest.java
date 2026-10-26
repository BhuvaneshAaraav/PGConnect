package com.pgconnect.billing;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflinePaymentRequestValidationTest {
    @Test
    void missingPaymentMethodIsAValidationErrorInsteadOfAValidatorCrash() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new BillingService.OfflinePaymentRequest(1000, "idempotency-key", null, null);
            var violations = factory.getValidator().validate(request);

            assertEquals(1, violations.size());
            assertTrue(violations.iterator().next().getPropertyPath().toString().contains("method"));
        }
    }
}
