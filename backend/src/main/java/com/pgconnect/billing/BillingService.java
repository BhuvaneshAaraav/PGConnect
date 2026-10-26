package com.pgconnect.billing;

import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.common.AuditLog;
import com.pgconnect.management.NotificationRepository;
import com.pgconnect.management.ResidentNotification;
import com.pgconnect.inventory.Bed;
import com.pgconnect.inventory.BedRepository;
import com.pgconnect.inventory.BedStatus;
import com.pgconnect.inventory.BuildingRepository;
import com.pgconnect.inventory.Room;
import com.pgconnect.inventory.RoomRepository;
import com.pgconnect.property.PgProperty;
import com.pgconnect.property.PgPropertyRepository;
import com.pgconnect.tenancy.Tenancy;
import com.pgconnect.tenancy.TenancyRepository;
import com.pgconnect.tenancy.TenancyStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {
    private final RentChargeRepository charges;
    private final RentPaymentRepository payments;
    private final TenancyRepository tenancies;
    private final BedRepository beds;
    private final RoomRepository rooms;
    private final BuildingRepository buildings;
    private final PgPropertyRepository properties;
    private final UserAccountRepository users;
    private final MongoTemplate mongoTemplate;
    private final NotificationRepository notifications;

    public BillingService(RentChargeRepository charges, RentPaymentRepository payments, TenancyRepository tenancies,
                          BedRepository beds, RoomRepository rooms, BuildingRepository buildings,
                          PgPropertyRepository properties, UserAccountRepository users, MongoTemplate mongoTemplate, NotificationRepository notifications) {
        this.charges = charges; this.payments = payments; this.tenancies = tenancies; this.beds = beds;
        this.rooms = rooms; this.buildings = buildings; this.properties = properties; this.users = users; this.mongoTemplate = mongoTemplate; this.notifications=notifications;
    }

    public int generate(String ownerId, String propertyId, String billingPeriod) {
        properties.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        YearMonth period = parsePeriod(billingPeriod);
        LocalDate start = period.atDay(1), end = period.atEndOfMonth();
        int created = 0;
        for (Tenancy tenancy : tenancies.findByOwnerIdAndPropertyIdAndStatusIn(ownerId, propertyId,
                List.of(TenancyStatus.ACTIVE, TenancyStatus.MOVE_OUT_REQUESTED))) {
            if (tenancy.getStartDate().isAfter(end) || (tenancy.getEndDate() != null && tenancy.getEndDate().isBefore(start))) continue;
            if (charges.findByTenancyIdAndBillingPeriod(tenancy.getId(), billingPeriod).isPresent()) continue;
            RentCharge charge = new RentCharge();
            charge.setOwnerId(ownerId); charge.setResidentId(tenancy.getResidentId()); charge.setTenancyId(tenancy.getId());
            charge.setPropertyId(propertyId); charge.setBuildingId(tenancy.getBuildingId()); charge.setRoomId(tenancy.getRoomId());
            charge.setBedId(tenancy.getBedId()); charge.setBillingPeriod(billingPeriod); charge.setRentAmount(tenancy.getMonthlyRent());
            charge.setPaidAmount(0); charge.setOutstandingBalance(tenancy.getMonthlyRent());
            charge.setDueDate(period.atDay(Math.min(5, period.lengthOfMonth()))); charge.setStatus(RentChargeStatus.UNPAID);
            charge.setCreatedAt(Instant.now()); charge.setUpdatedAt(Instant.now());
            try { charges.save(charge); created++; }
            catch (DuplicateKeyException ignored) { /* idempotent concurrent generation */ }
        }
        return created;
    }

    public List<RentChargeView> ownerCharges(String ownerId, String propertyId, String period) {
        properties.findByIdAndOwnerId(propertyId, ownerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        YearMonth parsed = parsePeriod(period);
        return charges.findByOwnerIdAndPropertyIdAndBillingPeriodOrderByBuildingIdAscRoomIdAsc(ownerId, propertyId, parsed.toString())
                .stream().map(this::toView).toList();
    }

    public List<RentChargeView> residentCharges(String residentId) {
        return charges.findByResidentIdOrderByBillingPeriodDesc(residentId).stream().map(this::toView).toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public RentPayment recordOfflinePayment(String ownerId, String propertyId, String chargeId,
                                            OfflinePaymentRequest request) {
        RentCharge charge = charges.findByIdAndOwnerId(chargeId, ownerId)
                .filter(value -> value.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent charge not found"));
        if (request.method() == PaymentMethod.GATEWAY) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gateway payments cannot be manually recorded");
        RentPayment existing = payments.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (existing.getChargeId().equals(chargeId) && existing.getAmount() == request.amount() && existing.getOwnerId().equals(ownerId)) return existing;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used for another payment");
        }
        if (request.amount() <= 0 || request.amount() > charge.getOutstandingBalance()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be positive and cannot exceed the outstanding balance");
        }
        RentPayment payment = new RentPayment();
        payment.setOwnerId(ownerId); payment.setResidentId(charge.getResidentId()); payment.setTenancyId(charge.getTenancyId());
        payment.setChargeId(chargeId); payment.setPropertyId(propertyId); payment.setAmount(request.amount());
        payment.setMethod(request.method()); payment.setStatus(RentPaymentStatus.CONFIRMED); payment.setReference(request.reference());
        payment.setRecordedBy(ownerId); payment.setIdempotencyKey(request.idempotencyKey()); payment.setGatewayVerified(false);
        payment.setConfirmedAt(Instant.now()); payment.setReceiptNumber("PG-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        try { payment = payments.save(payment); }
        catch (DuplicateKeyException duplicate) {
            return payments.findByIdempotencyKey(request.idempotencyKey()).filter(value -> value.getChargeId().equals(chargeId)
                            && value.getAmount() == request.amount())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used"));
        }

        long nextPaid = charge.getPaidAmount() + request.amount();
        long nextBalance = charge.getOutstandingBalance() - request.amount();
        RentChargeStatus nextStatus = nextBalance == 0 ? RentChargeStatus.PAID : RentChargeStatus.PARTIALLY_PAID;
        Query chargeStillOwed = Query.query(Criteria.where("_id").is(chargeId).and("ownerId").is(ownerId)
                .and("outstandingBalance").gte(request.amount()));
        RentCharge changed = mongoTemplate.findAndModify(chargeStillOwed, new Update().inc("paidAmount", request.amount())
                .inc("outstandingBalance", -request.amount()).set("status", nextStatus).set("updatedAt", Instant.now()), RentCharge.class);
        if (changed == null) {
            payments.deleteById(payment.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Charge balance changed. Refresh and retry with a new payment key.");
        }
        PaymentAllocation allocation = new PaymentAllocation(payment.getId(), chargeId, request.amount());
        mongoTemplate.save(allocation);
        Map<String, String> details = new HashMap<>();
        details.put("amount", Long.toString(request.amount())); details.put("method", request.method().name());
        details.put("billingPeriod", charge.getBillingPeriod()); details.put("paymentId", payment.getId());
        mongoTemplate.save(new AuditLog(ownerId, "OFFLINE_RENT_PAYMENT_RECORDED", "RentCharge", chargeId, details));
        return payment;
    }

    public List<UnpaidRow> unpaidReport(String ownerId, String propertyId, String buildingId,
                                        String roomId, String bedId, String residentId,
                                        String billingPeriod, boolean overdueOnly) {
        Query query = Query.query(Criteria.where("ownerId").is(ownerId).and("outstandingBalance").gt(0));
        if (propertyId != null && !propertyId.isBlank()) query.addCriteria(Criteria.where("propertyId").is(propertyId));
        if (buildingId != null && !buildingId.isBlank()) query.addCriteria(Criteria.where("buildingId").is(buildingId));
        if (roomId != null && !roomId.isBlank()) query.addCriteria(Criteria.where("roomId").is(roomId));
        if (bedId != null && !bedId.isBlank()) query.addCriteria(Criteria.where("bedId").is(bedId));
        if (residentId != null && !residentId.isBlank()) query.addCriteria(Criteria.where("residentId").is(residentId));
        if (billingPeriod != null && !billingPeriod.isBlank()) query.addCriteria(Criteria.where("billingPeriod").is(parsePeriod(billingPeriod).toString()));
        if (overdueOnly) query.addCriteria(Criteria.where("dueDate").lt(LocalDate.now()));
        query.limit(1000);
        return mongoTemplate.find(query, RentCharge.class).stream().map(charge -> {
            RentChargeView view = toView(charge);
            return new UnpaidRow(view, view.residentName(), view.propertyName(), view.buildingName(), view.roomNumber(), view.bedCode());
        }).toList();
    }

    public List<BedRentStatus> bedStatuses(String ownerId, String propertyId, String billingPeriod) {
        properties.findByIdAndOwnerId(propertyId, ownerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        parsePeriod(billingPeriod);
        List<BedRentStatus> result = new ArrayList<>();
        for (Bed bed : beds.findByOwnerIdAndPropertyIdOrderByBuildingIdAscRoomIdAscBedCodeAsc(ownerId, propertyId)) {
            Room room = rooms.findById(bed.getRoomId()).orElse(null);
            String buildingName = buildings.findById(bed.getBuildingId()).map(value -> value.getName() + " (" + value.getCode() + ")").orElse(null);
            if (bed.getStatus() != BedStatus.OCCUPIED) {
                String indicator = switch (bed.getStatus()) {
                    case RESERVED -> "RESERVED";
                    case MAINTENANCE -> "MAINTENANCE";
                    case UNAVAILABLE -> "UNAVAILABLE";
                    default -> "VACANT";
                };
                result.add(new BedRentStatus(propertyId, bed.getBuildingId(), buildingName, room == null ? null : room.getFloorNumber(),
                        bed.getRoomId(), room == null ? null : room.getRoomNumber(), bed.getId(), bed.getBedCode(), null, null, null,
                        room == null ? 0 : room.getMonthlyRent(), billingPeriod, null, 0, 0, 0, indicator));
                continue;
            }
            RentCharge charge = bed.getTenancyId() == null ? null : charges.findByTenancyIdAndBillingPeriod(bed.getTenancyId(), billingPeriod).orElse(null);
            UserAccount resident = bed.getTenancyId() == null ? null : tenancies.findById(bed.getTenancyId()).flatMap(value -> users.findById(value.getResidentId())).orElse(null);
            String indicator = charge == null ? "NOT_BILLED" : chargeIndicator(charge);
            result.add(new BedRentStatus(propertyId, bed.getBuildingId(), buildingName, room == null ? null : room.getFloorNumber(),
                    bed.getRoomId(), room == null ? null : room.getRoomNumber(), bed.getId(), bed.getBedCode(),
                    resident == null ? null : resident.getId(), resident == null ? null : resident.getName(),
                    resident == null ? null : resident.getEmailNormalized(), room == null ? 0 : room.getMonthlyRent(),
                    billingPeriod, charge == null ? null : charge.getDueDate(), charge == null ? 0 : charge.getRentAmount(),
                    charge == null ? 0 : charge.getPaidAmount(), charge == null ? 0 : charge.getOutstandingBalance(), indicator));
        }
        return result;
    }

    public List<RentPayment> residentPayments(String residentId) { return payments.findByResidentIdOrderByConfirmedAtDesc(residentId); }

    public Receipt receiptForResident(String residentId, String paymentId) {
        RentPayment payment = payments.findByIdAndResidentId(paymentId, residentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        RentCharge charge = charges.findById(payment.getChargeId()).orElse(null);
        return new Receipt(payment.getReceiptNumber(), payment.getId(), payment.getAmount(), payment.getMethod(),
                payment.getStatus(), payment.getConfirmedAt(), payment.isGatewayVerified(),
                charge == null ? null : charge.getBillingPeriod(), charge == null ? null : charge.getPropertyId());
    }

    @org.springframework.transaction.annotation.Transactional
    public void sendReminder(String ownerId,String propertyId,String chargeId){
        RentCharge charge=charges.findByIdAndOwnerId(chargeId,ownerId).filter(c->c.getPropertyId().equals(propertyId))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rent charge not found"));
        if(charge.getOutstandingBalance()<=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"This rent charge is fully paid");
        ResidentNotification notification=new ResidentNotification();notification.setUserId(charge.getResidentId());notification.setType("RENT_REMINDER");
        notification.setTitle("Rent payment reminder");notification.setMessage("Rent for "+charge.getBillingPeriod()+" has an outstanding balance of ₹"+charge.getOutstandingBalance()+" due on "+charge.getDueDate()+".");
        notification.setEntityType("RentCharge");notification.setEntityId(chargeId);notifications.save(notification);
        mongoTemplate.save(new AuditLog(ownerId,"RENT_REMINDER_SENT","RentCharge",chargeId,Map.of("billingPeriod",charge.getBillingPeriod(),"balance",Long.toString(charge.getOutstandingBalance()))));
    }

    public int generateForPeriod(String billingPeriod) {
        int count = 0;
        for (UserAccount owner : users.findAll().stream().filter(account -> account.getRole() == com.pgconnect.auth.Role.OWNER).toList()) {
            for (PgProperty property : properties.findByOwnerIdOrderByCreatedAtDesc(owner.getId())) {
                count += generate(owner.getId(), property.getId(), billingPeriod);
            }
        }
        return count;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 10 0 1 * *", zone = "Asia/Kolkata")
    public void generateCurrentMonthCharges() { generateForPeriod(YearMonth.now(java.time.ZoneId.of("Asia/Kolkata")).toString()); }

    private RentChargeView toView(RentCharge charge) {
        UserAccount resident = users.findById(charge.getResidentId()).orElse(null);
        PgProperty property = properties.findById(charge.getPropertyId()).orElse(null);
        Room room = rooms.findById(charge.getRoomId()).orElse(null);
        Bed bed = beds.findById(charge.getBedId()).orElse(null);
        String buildingName = buildings.findById(charge.getBuildingId()).map(value -> value.getName() + " (" + value.getCode() + ")").orElse(null);
        return new RentChargeView(charge.getId(), charge.getResidentId(), resident == null ? null : resident.getName(),
                charge.getTenancyId(), charge.getPropertyId(), property == null ? null : property.getName(),
                charge.getBuildingId(), buildingName, charge.getRoomId(), room == null ? null : room.getRoomNumber(),
                charge.getBedId(), bed == null ? null : bed.getBedCode(), charge.getBillingPeriod(), charge.getRentAmount(),
                charge.getPaidAmount(), charge.getOutstandingBalance(), charge.getDueDate(), charge.getStatus(), chargeIndicator(charge));
    }

    private String chargeIndicator(RentCharge charge) {
        if (charge.getOutstandingBalance() == 0) return "PAID";
        if (charge.getDueDate().isBefore(LocalDate.now())) return "OVERDUE";
        if (!charge.getDueDate().isAfter(LocalDate.now().plusDays(3))) return "DUE_SOON";
        return charge.getPaidAmount() > 0 ? "PARTIALLY_PAID" : "DUE";
    }

    private YearMonth parsePeriod(String billingPeriod) {
        try { return YearMonth.parse(billingPeriod); }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billing period must use YYYY-MM"); }
    }

    public record OfflinePaymentRequest(@Min(1) @Max(10000000) long amount,
                                        @NotBlank @Size(max = 80) String idempotencyKey,
                                        @NotNull PaymentMethod method,
                                        @Size(max = 160) String reference) {}
    public record RentChargeView(String id, String residentId, String residentName, String tenancyId,
                                 String propertyId, String propertyName, String buildingId, String buildingName,
                                 String roomId, String roomNumber, String bedId, String bedCode, String billingPeriod,
                                 long rentAmount, long paidAmount, long outstandingBalance, LocalDate dueDate,
                                 RentChargeStatus status, String indicator) {}
    public record UnpaidRow(RentChargeView charge, String residentName, String propertyName,
                            String buildingName, String roomNumber, String bedCode) {}
    public record BedRentStatus(String propertyId, String buildingId, String buildingName, Integer floorNumber,
                                String roomId, String roomNumber, String bedId, String bedCode,
                                String residentId, String residentName, String residentEmail, long monthlyRent,
                                String billingPeriod, LocalDate dueDate, long rentAmount, long paidAmount, long outstandingBalance,
                                String indicator) {}
    public record Receipt(String receiptNumber, String paymentId, long amount, PaymentMethod method,
                          RentPaymentStatus status, Instant confirmedAt, boolean gatewayVerified,
                          String billingPeriod, String propertyId) {}
}
