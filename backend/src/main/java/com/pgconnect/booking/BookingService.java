package com.pgconnect.booking;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.SubscriptionStatus;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.inventory.Bed;
import com.pgconnect.inventory.BedRepository;
import com.pgconnect.inventory.BedStatus;
import com.pgconnect.inventory.Building;
import com.pgconnect.inventory.BuildingRepository;
import com.pgconnect.inventory.Room;
import com.pgconnect.inventory.RoomRepository;
import com.pgconnect.property.PgProperty;
import com.pgconnect.property.PgPropertyRepository;
import com.pgconnect.property.PublicListingPolicy;
import com.pgconnect.tenancy.Tenancy;
import com.pgconnect.tenancy.TenancyRepository;
import com.pgconnect.tenancy.TenancyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final BedRepository beds;
    private final RoomRepository rooms;
    private final PgPropertyRepository properties;
    private final UserAccountRepository users;
    private final TenancyRepository tenancies;
    private final BuildingRepository buildings;
    private final MongoTemplate mongoTemplate;

    public BookingService(BookingRepository bookings, BedRepository beds, RoomRepository rooms,
                          PgPropertyRepository properties, UserAccountRepository users,
                          TenancyRepository tenancies, BuildingRepository buildings, MongoTemplate mongoTemplate) {
        this.bookings = bookings; this.beds = beds; this.rooms = rooms; this.properties = properties;
        this.users = users; this.tenancies = tenancies; this.buildings = buildings; this.mongoTemplate = mongoTemplate;
    }

    public List<AvailableRoom> availableBeds(String propertyId) {
        PgProperty property = properties.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        UserAccount owner = users.findById(property.getOwnerId()).orElse(null);
        if (!PublicListingPolicy.isEligible(property, owner)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        return rooms.findByPropertyIdOrderByBuildingIdAscFloorNumberAscRoomNumberAsc(propertyId).stream()
                .map(room -> new AvailableRoom(room.getId(), room.getBuildingId(), buildings.findById(room.getBuildingId()).map(Building::getName).orElse("Building"), room.getFloorNumber(), room.getRoomNumber(),
                        room.getSharingType(), room.getMonthlyRent(), beds.findByRoomIdOrderByBedCodeAsc(room.getId()).stream()
                        .filter(bed -> bed.getStatus() == BedStatus.AVAILABLE)
                        .map(bed -> new AvailableBed(bed.getId(), bed.getBedCode())).toList()))
                .filter(room -> !room.beds().isEmpty()).toList();
    }

    @Transactional
    public Booking requestBooking(String residentId, String propertyId, String bedId, BookingRequest request) {
        UserAccount resident = users.findById(residentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (resident.getRole() != Role.RESIDENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resident account required");
        if (!resident.isEmailVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verify your email before requesting a bed");
        PgProperty property = properties.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        UserAccount owner = users.findById(property.getOwnerId()).orElse(null);
        if (!PublicListingPolicy.isEligible(property, owner)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        Bed bed = beds.findById(bedId).filter(candidate -> candidate.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bed not found"));
        if (bed.getStatus() != BedStatus.AVAILABLE) throw new ResponseStatusException(HttpStatus.CONFLICT, "That bed is no longer available");

        Instant now = Instant.now();
        Booking booking = new Booking();
        booking.setResidentId(residentId); booking.setOwnerId(property.getOwnerId());
        booking.setPropertyId(propertyId); booking.setBuildingId(bed.getBuildingId());
        booking.setRoomId(bed.getRoomId()); booking.setBedId(bedId);
        booking.setResidentMessage(request.message() == null ? null : request.message().trim());
        booking.setStatus(BookingStatus.REQUESTED); booking.setExpiresAt(now.plusSeconds(24 * 60 * 60));
        booking.setCreatedAt(now); booking.setUpdatedAt(now);
        Booking saved = bookings.save(booking);
        Query bedIsFree = Query.query(Criteria.where("_id").is(bedId).and("propertyId").is(propertyId).and("status").is(BedStatus.AVAILABLE));
        Bed reserved = mongoTemplate.findAndModify(bedIsFree, new Update().set("status", BedStatus.RESERVED)
                .set("bookingId", saved.getId()).set("reservedUntil", saved.getExpiresAt()), Bed.class);
        if (reserved == null) {
            bookings.deleteById(saved.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That bed was just reserved by someone else");
        }
        return saved;
    }

    public List<ResidentBookingView> residentBookings(String residentId) {
        return bookings.findByResidentIdOrderByCreatedAtDesc(residentId).stream().map(booking -> {
            PgProperty property=properties.findById(booking.getPropertyId()).orElse(null);
            Building building=buildings.findById(booking.getBuildingId()).orElse(null);
            Room room=rooms.findById(booking.getRoomId()).orElse(null);
            Bed bed=beds.findById(booking.getBedId()).orElse(null);
            return new ResidentBookingView(booking.getId(),booking.getPropertyId(),property==null?null:property.getName(),
                    building==null?null:building.getName(),room==null?null:room.getRoomNumber(),bed==null?null:bed.getBedCode(),
                    booking.getStatus(),booking.getResidentMessage(),booking.getOwnerResponse(),booking.getExpiresAt(),booking.getCreatedAt());
        }).toList();
    }
    public List<OwnerBookingView> ownerBookings(String ownerId, String propertyId) {
        properties.findByIdAndOwnerId(propertyId, ownerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        return bookings.findByOwnerIdAndPropertyIdOrderByCreatedAtDesc(ownerId, propertyId).stream().map(booking -> {
            UserAccount resident=users.findById(booking.getResidentId()).orElse(null);
            Building building=buildings.findById(booking.getBuildingId()).orElse(null);
            Room room=rooms.findById(booking.getRoomId()).orElse(null);
            Bed bed=beds.findById(booking.getBedId()).orElse(null);
            return new OwnerBookingView(booking.getId(),booking.getResidentId(),resident==null?null:resident.getName(),resident==null?null:resident.getEmailNormalized(),
                    booking.getPropertyId(),booking.getBuildingId(),building==null?null:building.getName(),booking.getRoomId(),room==null?null:room.getRoomNumber(),booking.getBedId(),bed==null?null:bed.getBedCode(),booking.getStatus(),booking.getResidentMessage(),booking.getOwnerResponse(),booking.getExpiresAt(),booking.getCreatedAt());
        }).toList();
    }

    @Transactional
    public Booking decide(String ownerId, String propertyId, String bookingId, BookingDecision request) {
        Booking booking = bookings.findByIdAndOwnerIdAndPropertyId(bookingId, ownerId, propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (booking.getStatus() != BookingStatus.REQUESTED || booking.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking is no longer awaiting a decision");
        }
        BookingStatus next = request.approve() ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        Query pending = Query.query(Criteria.where("_id").is(bookingId).and("status").is(BookingStatus.REQUESTED)
                .and("expiresAt").gt(Instant.now()));
        Booking changed = mongoTemplate.findAndModify(pending, new Update().set("status", next)
                .set("ownerResponse", request.response() == null ? null : request.response().trim())
                .set("updatedAt", Instant.now()), Booking.class);
        if (changed == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking changed. Refresh and try again.");
        changed.setStatus(next);
        if (next == BookingStatus.REJECTED) releaseBed(booking, BedStatus.RESERVED);
        return changed;
    }

    @Transactional
    public Tenancy moveIn(String ownerId, String propertyId, String bookingId, MoveInRequest request) {
        Booking booking = bookings.findByIdAndOwnerIdAndPropertyId(bookingId, ownerId, propertyId)
                .filter(candidate -> candidate.getStatus() == BookingStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved booking not found"));
        Room room = rooms.findById(booking.getRoomId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Room record missing"));
        Tenancy tenancy = new Tenancy();
        tenancy.setOwnerId(ownerId); tenancy.setResidentId(booking.getResidentId());
        tenancy.setPropertyId(propertyId); tenancy.setBuildingId(booking.getBuildingId());
        tenancy.setRoomId(booking.getRoomId()); tenancy.setBedId(booking.getBedId());
        tenancy.setBookingId(bookingId); tenancy.setMonthlyRent(room.getMonthlyRent());
        tenancy.setStartDate(request.startDate() == null ? LocalDate.now() : request.startDate());
        tenancy.setExpectedEndDate(request.expectedEndDate()); tenancy.setStatus(TenancyStatus.ACTIVE);
        tenancy.setCreatedAt(Instant.now()); tenancy.setUpdatedAt(Instant.now());
        Tenancy saved = tenancies.save(tenancy);
        Query reservation = Query.query(Criteria.where("_id").is(booking.getBedId()).and("status").is(BedStatus.RESERVED).and("bookingId").is(bookingId));
        Bed changed = mongoTemplate.findAndModify(reservation, new Update().set("status", BedStatus.OCCUPIED)
                .set("tenancyId", saved.getId()).unset("bookingId").unset("reservedUntil"), Bed.class);
        if (changed == null) {
            tenancies.deleteById(saved.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bed reservation expired or changed. Refresh and try again.");
        }
        booking.setStatus(BookingStatus.MOVED_IN); booking.setUpdatedAt(Instant.now()); bookings.save(booking);
        return saved;
    }

    @Transactional
    public Booking cancel(String residentId, String bookingId) {
        Booking booking = bookings.findByIdAndResidentId(bookingId, residentId)
                .filter(candidate -> candidate.getStatus() == BookingStatus.REQUESTED || candidate.getStatus() == BookingStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active booking not found"));
        BookingStatus previous = booking.getStatus();
        Query active = Query.query(Criteria.where("_id").is(bookingId).and("status").is(previous));
        Booking cancelled = mongoTemplate.findAndModify(active, new Update().set("status", BookingStatus.CANCELLED)
                .set("updatedAt", Instant.now()), Booking.class);
        if (cancelled == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking changed. Refresh and try again.");
        releaseBed(booking, BedStatus.RESERVED);
        cancelled.setStatus(BookingStatus.CANCELLED);
        return cancelled;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${pgconnect.booking.expiry-scan-ms:60000}")
    public void expireReservations() {
        Instant now = Instant.now();
        for (Booking booking : bookings.findByStatusAndExpiresAtBefore(BookingStatus.REQUESTED, now)) expire(booking);
        for (Booking booking : bookings.findByStatusAndExpiresAtBefore(BookingStatus.APPROVED, now)) expire(booking);
        for (Bed bed : beds.findByStatusAndReservedUntilBefore(BedStatus.RESERVED, now)) {
            if (bed.getBookingId() == null) {
                Query expiredBed = Query.query(Criteria.where("_id").is(bed.getId()).and("status").is(BedStatus.RESERVED).and("reservedUntil").lt(now));
                mongoTemplate.updateFirst(expiredBed, new Update().set("status", BedStatus.AVAILABLE).unset("reservedUntil"), Bed.class);
            }
        }
    }

    private void expire(Booking booking) {
        Query active = Query.query(Criteria.where("_id").is(booking.getId()).and("status").is(booking.getStatus()).and("expiresAt").lt(Instant.now()));
        Booking expired = mongoTemplate.findAndModify(active, new Update().set("status", BookingStatus.EXPIRED).set("updatedAt", Instant.now()), Booking.class);
        if (expired != null) releaseBed(booking, BedStatus.RESERVED);
    }

    private void releaseBed(Booking booking, BedStatus expectedStatus) {
        Query reserve = Query.query(Criteria.where("_id").is(booking.getBedId()).and("bookingId").is(booking.getId()).and("status").is(expectedStatus));
        mongoTemplate.updateFirst(reserve, new Update().set("status", BedStatus.AVAILABLE).unset("bookingId").unset("reservedUntil"), Bed.class);
    }

    public record BookingRequest(@Size(max = 500) String message) {}
    public record BookingDecision(boolean approve, @Size(max = 500) String response) {}
    public record MoveInRequest(java.time.LocalDate startDate, java.time.LocalDate expectedEndDate) {}
    public record AvailableBed(String id, String bedCode) {}
    public record AvailableRoom(String id, String buildingId,String buildingName, int floorNumber, String roomNumber,
                                com.pgconnect.inventory.SharingType sharingType, long monthlyRent,
                                List<AvailableBed> beds) {}
    public record ResidentBookingView(String id,String propertyId,String propertyName,String buildingName,String roomNumber,String bedCode,
                                      BookingStatus status,String residentMessage,String ownerResponse,Instant expiresAt,Instant createdAt){}
    public record OwnerBookingView(String id,String residentId,String residentName,String residentEmail,String propertyId,String buildingId,String buildingName,String roomId,String roomNumber,String bedId,String bedCode,
                                   BookingStatus status,String residentMessage,String ownerResponse,Instant expiresAt,Instant createdAt){}
}
