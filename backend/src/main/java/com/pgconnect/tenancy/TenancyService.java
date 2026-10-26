package com.pgconnect.tenancy;

import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.inventory.Bed;
import com.pgconnect.inventory.BedRepository;
import com.pgconnect.inventory.BedStatus;
import com.pgconnect.inventory.BuildingRepository;
import com.pgconnect.inventory.Room;
import com.pgconnect.inventory.RoomRepository;
import com.pgconnect.property.PgProperty;
import com.pgconnect.property.PgPropertyRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class TenancyService {
    private final TenancyRepository tenancies;
    private final UserAccountRepository users;
    private final PgPropertyRepository properties;
    private final BuildingRepository buildings;
    private final RoomRepository rooms;
    private final BedRepository beds;
    private final MongoTemplate mongoTemplate;

    public TenancyService(TenancyRepository tenancies, UserAccountRepository users, PgPropertyRepository properties,
                          BuildingRepository buildings, RoomRepository rooms, BedRepository beds, MongoTemplate mongoTemplate) {
        this.tenancies = tenancies; this.users = users; this.properties = properties; this.buildings = buildings;
        this.rooms = rooms; this.beds = beds; this.mongoTemplate = mongoTemplate;
    }

    public Accommodation current(String residentId) {
        Tenancy tenancy = tenancies.findFirstByResidentIdAndStatusInOrderByStartDateDesc(residentId,
                        List.of(TenancyStatus.ACTIVE, TenancyStatus.MOVE_OUT_REQUESTED))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No current accommodation"));
        return toAccommodation(tenancy);
    }

    public List<Accommodation> residentHistory(String residentId) {
        return tenancies.findByResidentIdOrderByStartDateDesc(residentId).stream().map(this::toAccommodation).toList();
    }

    public List<Accommodation> ownerResidents(String ownerId, String propertyId) {
        properties.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        return tenancies.findByOwnerIdAndPropertyIdAndStatusIn(ownerId, propertyId,
                        List.of(TenancyStatus.ACTIVE, TenancyStatus.MOVE_OUT_REQUESTED))
                .stream().map(this::toAccommodation).toList();
    }

    @Transactional
    public Tenancy requestMoveOut(String residentId, String tenancyId, MoveOutRequest request) {
        Tenancy tenancy = tenancies.findById(tenancyId).filter(value -> value.getResidentId().equals(residentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenancy not found"));
        if (tenancy.getStatus() != TenancyStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenancy is not active");
        Query active = Query.query(Criteria.where("_id").is(tenancyId).and("status").is(TenancyStatus.ACTIVE));
        Tenancy changed = mongoTemplate.findAndModify(active, new Update().set("status", TenancyStatus.MOVE_OUT_REQUESTED)
                .set("moveOutRequestedDate", request.requestedDate() == null ? LocalDate.now() : request.requestedDate())
                .set("moveOutReason", request.reason()).set("updatedAt", Instant.now()), Tenancy.class);
        if (changed == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenancy changed. Refresh and try again.");
        changed.setStatus(TenancyStatus.MOVE_OUT_REQUESTED);
        return changed;
    }

    @Transactional
    public Tenancy completeMoveOut(String ownerId, String propertyId, String tenancyId, MoveOutDecision request) {
        Tenancy tenancy = tenancies.findByIdAndOwnerIdAndPropertyId(tenancyId, ownerId, propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenancy not found"));
        if (tenancy.getStatus() != TenancyStatus.MOVE_OUT_REQUESTED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Move-out has not been requested");
        if (!request.approve()) {
            Query pending = Query.query(Criteria.where("_id").is(tenancyId).and("status").is(TenancyStatus.MOVE_OUT_REQUESTED));
            Tenancy reset = mongoTemplate.findAndModify(pending, new Update().set("status", TenancyStatus.ACTIVE)
                    .set("updatedAt", Instant.now()), Tenancy.class);
            if (reset == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenancy changed. Refresh and try again.");
            reset.setStatus(TenancyStatus.ACTIVE);
            return reset;
        }
        Query occupied = Query.query(Criteria.where("_id").is(tenancy.getBedId()).and("ownerId").is(ownerId)
                .and("status").is(BedStatus.OCCUPIED).and("tenancyId").is(tenancyId));
        Bed bed = mongoTemplate.findAndModify(occupied, new Update().set("status", BedStatus.AVAILABLE)
                .unset("tenancyId"), Bed.class);
        if (bed == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Bed assignment changed. Contact support before completing move-out.");
        Query pending = Query.query(Criteria.where("_id").is(tenancyId).and("status").is(TenancyStatus.MOVE_OUT_REQUESTED));
        Tenancy ended = mongoTemplate.findAndModify(pending, new Update().set("status", TenancyStatus.ENDED)
                .set("endDate", request.endDate() == null ? LocalDate.now() : request.endDate())
                .set("updatedAt", Instant.now()), Tenancy.class);
        if (ended == null) {
            Query restore = Query.query(Criteria.where("_id").is(tenancy.getBedId()).and("status").is(BedStatus.AVAILABLE));
            mongoTemplate.updateFirst(restore, new Update().set("status", BedStatus.OCCUPIED).set("tenancyId", tenancyId), Bed.class);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenancy changed while move-out was being completed");
        }
        ended.setStatus(TenancyStatus.ENDED);
        ended.setEndDate(request.endDate() == null ? LocalDate.now() : request.endDate());
        return ended;
    }

    private Accommodation toAccommodation(Tenancy tenancy) {
        UserAccount resident = users.findById(tenancy.getResidentId()).orElse(null);
        PgProperty property = properties.findById(tenancy.getPropertyId()).orElse(null);
        Room room = rooms.findById(tenancy.getRoomId()).orElse(null);
        Bed bed = beds.findById(tenancy.getBedId()).orElse(null);
        String buildingName = buildings.findById(tenancy.getBuildingId()).map(value -> value.getName() + " (" + value.getCode() + ")").orElse(null);
        return new Accommodation(tenancy.getId(), tenancy.getResidentId(), resident == null ? null : resident.getName(),
                resident == null ? null : resident.getEmailNormalized(), tenancy.getPropertyId(), property == null ? null : property.getName(),
                tenancy.getBuildingId(), buildingName, tenancy.getRoomId(), room == null ? null : room.getRoomNumber(),
                tenancy.getBedId(), bed == null ? null : bed.getBedCode(), tenancy.getMonthlyRent(),
                tenancy.getStartDate(), tenancy.getExpectedEndDate(), tenancy.getEndDate(), tenancy.getStatus());
    }

    public record MoveOutRequest(LocalDate requestedDate, String reason) {}
    public record MoveOutDecision(boolean approve, LocalDate endDate) {}
    public record Accommodation(String tenancyId, String residentId, String residentName, String residentEmail,
                                String propertyId, String propertyName, String buildingId, String buildingName,
                                String roomId, String roomNumber, String bedId, String bedCode, long monthlyRent,
                                LocalDate startDate, LocalDate expectedEndDate, LocalDate endDate, TenancyStatus status) {}
}
