package com.pgconnect.billing;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BillingController {
    private final BillingService billing;
    public BillingController(BillingService billing) { this.billing = billing; }

    @PostMapping("/api/owner/properties/{propertyId}/rent/charges/generate")
    @PreAuthorize("hasRole('OWNER')")
    public int generate(@PathVariable String propertyId, @RequestParam String billingPeriod, Authentication auth) {
        return billing.generate(auth.getName(), propertyId, billingPeriod);
    }

    @GetMapping("/api/owner/properties/{propertyId}/rent/charges")
    @PreAuthorize("hasRole('OWNER')")
    public List<BillingService.RentChargeView> ownerCharges(@PathVariable String propertyId,
                                                             @RequestParam String billingPeriod, Authentication auth) {
        return billing.ownerCharges(auth.getName(), propertyId, billingPeriod);
    }

    @PostMapping("/api/owner/properties/{propertyId}/rent/charges/{chargeId}/offline-payment")
    @PreAuthorize("hasRole('OWNER')")
    public RentPayment recordOffline(@PathVariable String propertyId, @PathVariable String chargeId,
                                     @Valid @RequestBody BillingService.OfflinePaymentRequest request, Authentication auth) {
        return billing.recordOfflinePayment(auth.getName(), propertyId, chargeId, request);
    }

    @GetMapping("/api/owner/properties/{propertyId}/rent/bed-status")
    @PreAuthorize("hasRole('OWNER')")
    public List<BillingService.BedRentStatus> bedStatuses(@PathVariable String propertyId,
                                                          @RequestParam String billingPeriod, Authentication auth) {
        return billing.bedStatuses(auth.getName(), propertyId, billingPeriod);
    }

    @GetMapping("/api/owner/rent/outstanding")
    @PreAuthorize("hasRole('OWNER')")
    public List<BillingService.UnpaidRow> unpaid(@RequestParam(required = false) String propertyId,
                                                 @RequestParam(required = false) String buildingId,
                                                 @RequestParam(required = false) String roomId,
                                                 @RequestParam(required = false) String bedId,
                                                 @RequestParam(required = false) String residentId,
                                                 @RequestParam(required = false) String billingPeriod,
                                                 @RequestParam(defaultValue = "false") boolean overdueOnly,
                                                 Authentication auth) {
        return billing.unpaidReport(auth.getName(), propertyId, buildingId, roomId, bedId, residentId, billingPeriod, overdueOnly);
    }

    @PostMapping("/api/owner/properties/{propertyId}/rent/charges/{chargeId}/reminder")
    @PreAuthorize("hasRole('OWNER')")
    public void reminder(@PathVariable String propertyId,@PathVariable String chargeId,Authentication auth){billing.sendReminder(auth.getName(),propertyId,chargeId);}

    @GetMapping("/api/resident/rent/charges")
    @PreAuthorize("hasRole('RESIDENT')")
    public List<BillingService.RentChargeView> residentCharges(Authentication auth) { return billing.residentCharges(auth.getName()); }

    @GetMapping("/api/resident/rent/payments")
    @PreAuthorize("hasRole('RESIDENT')")
    public List<RentPayment> residentPayments(Authentication auth) { return billing.residentPayments(auth.getName()); }

    @GetMapping("/api/resident/rent/payments/{paymentId}/receipt")
    @PreAuthorize("hasRole('RESIDENT')")
    public BillingService.Receipt receipt(@PathVariable String paymentId, Authentication auth) { return billing.receiptForResident(auth.getName(), paymentId); }
}
