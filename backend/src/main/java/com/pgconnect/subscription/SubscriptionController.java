package com.pgconnect.subscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class SubscriptionController {
    private final SubscriptionService service; public SubscriptionController(SubscriptionService service){this.service=service;}
    @GetMapping("/api/owner/subscription") @PreAuthorize("hasRole('OWNER')") public OwnerSubscription current(Authentication a){return service.current(a.getName());}
    @GetMapping("/api/owner/subscription/invoices") @PreAuthorize("hasRole('OWNER')") public List<SubscriptionInvoice> invoices(Authentication a){return service.invoices(a.getName());}
    @PostMapping("/api/owner/subscription/checkout") @PreAuthorize("hasRole('OWNER')") public SubscriptionService.Checkout checkout(Authentication a){return service.start(a.getName());}
    @PostMapping("/api/owner/subscription/verify") @PreAuthorize("hasRole('OWNER')") public SubscriptionService.CheckoutResult verify(@Valid @RequestBody VerifyRequest request,Authentication a){return service.verifyCheckout(a.getName(),new SubscriptionService.CheckoutResultRequest(request.subscriptionId(),request.paymentId(),request.signature()));}
    @PostMapping("/api/owner/subscription/cancel") @PreAuthorize("hasRole('OWNER')") public SubscriptionService.CancellationResult cancel(Authentication a){return service.cancelAtCycleEnd(a.getName());}
    @PostMapping("/api/webhooks/razorpay") @ResponseStatus(HttpStatus.NO_CONTENT) public void webhook(@RequestBody String raw,@RequestHeader("X-Razorpay-Signature") String signature,@RequestHeader(value="X-Razorpay-Event-Id",required=false) String eventId){service.handleWebhook(raw,signature,eventId);}
    public record VerifyRequest(@NotBlank String subscriptionId,@NotBlank String paymentId,@NotBlank String signature){}
}
