package com.pgconnect.billing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RentGatewayController {
    private final RentGatewayService gateway;

    public RentGatewayController(RentGatewayService gateway) {
        this.gateway = gateway;
    }

    @PostMapping("/api/resident/rent/charges/{chargeId}/checkout")
    @PreAuthorize("hasRole('RESIDENT')")
    public RentGatewayService.Checkout checkout(@PathVariable String chargeId,
            @Valid @RequestBody RentGatewayService.CheckoutRequest request, Authentication authentication) {
        return gateway.start(authentication.getName(), chargeId, request);
    }

    @PostMapping("/api/resident/rent/payments/verify")
    @PreAuthorize("hasRole('RESIDENT')")
    public RentGatewayService.VerifyResult verify(@Valid @RequestBody RentGatewayService.VerifyRequest request,
            Authentication authentication) {
        return gateway.verify(authentication.getName(), request);
    }

    @PostMapping("/api/webhooks/razorpay/rent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rentWebhook(@RequestBody String raw,
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        gateway.webhook(raw, signature, eventId);
    }
}
