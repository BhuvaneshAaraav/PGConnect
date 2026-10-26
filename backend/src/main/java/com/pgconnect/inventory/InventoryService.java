package com.pgconnect.inventory;

import com.pgconnect.property.PgPropertyRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryService {
    private final PgPropertyRepository properties;
    private final BuildingRepository buildings;
    private final RoomRepository rooms;
    private final BedRepository beds;
    private final MongoTemplate mongoTemplate;

    public InventoryService(PgPropertyRepository properties, BuildingRepository buildings,
                            RoomRepository rooms, BedRepository beds, MongoTemplate mongoTemplate) {
        this.properties = properties; this.buildings = buildings; this.rooms = rooms;
        this.beds = beds; this.mongoTemplate = mongoTemplate;
    }

    public List<Building> listBuildings(String ownerId, String propertyId) {
        requireProperty(ownerId, propertyId);
        return buildings.findByOwnerIdAndPropertyIdOrderByCode(ownerId, propertyId);
    }

    public Building createBuilding(String ownerId, String propertyId, BuildingRequest request) {
        requireProperty(ownerId, propertyId);
        Building building = new Building();
        building.setOwnerId(ownerId);
        building.setPropertyId(propertyId);
        building.setName(request.name().trim());
        building.setCode(request.code().trim());
        building.setFloorCount(request.floorCount());
        return buildings.save(building);
    }

    public InventoryView inventory(String ownerId, String propertyId) {
        requireProperty(ownerId, propertyId);
        List<BuildingView> views = new ArrayList<>();
        for (Building building : buildings.findByOwnerIdAndPropertyIdOrderByCode(ownerId, propertyId)) {
            List<RoomView> roomViews = new ArrayList<>();
            for (Room room : rooms.findByOwnerIdAndPropertyIdAndBuildingIdOrderByFloorNumberAscRoomNumberAsc(ownerId, propertyId, building.getId())) {
                List<BedView> bedViews = beds.findByOwnerIdAndPropertyIdAndBuildingIdAndRoomIdOrderByBedCodeAsc(ownerId, propertyId, building.getId(), room.getId())
                        .stream().map(bed -> new BedView(bed.getId(), bed.getBedCode(), bed.getStatus(), bed.getBookingId(), bed.getTenancyId()))
                        .toList();
                roomViews.add(new RoomView(room.getId(), room.getFloorNumber(), room.getRoomNumber(), room.getSharingType(),
                        room.getMonthlyRent(), room.getFacilities(), room.getStatus(), bedViews));
            }
            views.add(new BuildingView(building.getId(), building.getName(), building.getCode(), building.getFloorCount(), roomViews));
        }
        long bedCount = beds.countByOwnerIdAndPropertyId(ownerId, propertyId);
        return new InventoryView(propertyId, views, bedCount,
                beds.countByOwnerIdAndPropertyIdAndStatus(ownerId, propertyId, BedStatus.OCCUPIED),
                beds.countByOwnerIdAndPropertyIdAndStatus(ownerId, propertyId, BedStatus.AVAILABLE),
                beds.countByOwnerIdAndPropertyIdAndStatus(ownerId, propertyId, BedStatus.RESERVED),
                beds.countByOwnerIdAndPropertyIdAndStatus(ownerId, propertyId, BedStatus.MAINTENANCE),
                beds.countByOwnerIdAndPropertyIdAndStatus(ownerId, propertyId, BedStatus.UNAVAILABLE));
    }

    @org.springframework.transaction.annotation.Transactional
    public Room createRoom(String ownerId, String propertyId, String buildingId, RoomRequest request) {
        requireProperty(ownerId, propertyId);
        Building building = buildings.findById(buildingId)
                .filter(candidate -> candidate.getOwnerId().equals(ownerId) && candidate.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Building not found"));
        if (request.floorNumber() >= building.getFloorCount()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Floor number exceeds this building’s configured floors");
        if (request.bedCodes().size() != request.sharingType().capacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add exactly one bed record for each place in the sharing type");
        }
        Set<String> normalizedCodes = new HashSet<>();
        for (String code : request.bedCodes()) {
            if (!normalizedCodes.add(code.trim().toLowerCase())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bed codes must be unique within a room");
        }

        Room room = new Room();
        room.setOwnerId(ownerId); room.setPropertyId(propertyId); room.setBuildingId(buildingId);
        room.setFloorNumber(request.floorNumber()); room.setRoomNumber(request.roomNumber().trim());
        room.setSharingType(request.sharingType()); room.setMonthlyRent(request.monthlyRent());
        room.setFacilities(request.facilities()); room.setStatus(RoomStatus.ACTIVE);
        Room saved = rooms.save(room);
        try {
            List<Bed> bedRecords = request.bedCodes().stream().map(code -> {
                Bed bed = new Bed(); bed.setOwnerId(ownerId); bed.setPropertyId(propertyId);
                bed.setBuildingId(buildingId); bed.setRoomId(saved.getId()); bed.setBedCode(code.trim());
                bed.setStatus(BedStatus.AVAILABLE); return bed;
            }).toList();
            beds.saveAll(bedRecords);
            return saved;
        } catch (RuntimeException exception) {
            beds.deleteByRoomId(saved.getId());
            rooms.deleteById(saved.getId());
            throw exception;
        }
    }

    public Bed updateBedStatus(String ownerId, String propertyId, String bedId, BedStatus nextStatus) {
        Bed current = beds.findByIdAndOwnerIdAndPropertyId(bedId, ownerId, propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bed not found"));
        if (nextStatus == BedStatus.RESERVED || nextStatus == BedStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation and occupancy states are set only by booking and move-in workflows");
        }
        if (current.getStatus() == BedStatus.RESERVED || current.getStatus() == BedStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A reserved or occupied bed must be released through its booking or tenancy workflow");
        }
        Query query = Query.query(Criteria.where("_id").is(bedId).and("ownerId").is(ownerId)
                .and("propertyId").is(propertyId).and("status").is(current.getStatus()));
        Bed changed = mongoTemplate.findAndModify(query, new Update().set("status", nextStatus)
                .set("updatedAt", Instant.now()), Bed.class);
        if (changed == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Bed state changed. Refresh and try again.");
        changed.setStatus(nextStatus);
        return changed;
    }

    private void requireProperty(String ownerId, String propertyId) {
        if (properties.findByIdAndOwnerId(propertyId, ownerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }
    }

    public record BuildingRequest(@NotBlank @Size(max = 80) String name,
                                  @NotBlank @Size(max = 20) String code,
                                  @Min(1) @Max(100) int floorCount) {}

    public record RoomRequest(@Min(0) int floorNumber,
                              @NotBlank @Size(max = 20) String roomNumber,
                              @NotNull SharingType sharingType,
                              @Min(1) @Max(10000000) long monthlyRent,
                              @NotNull @Size(min = 1, max = 4) List<@NotBlank @Size(max = 16) String> bedCodes,
                              @Size(max = 20) List<@NotBlank @Size(max = 40) String> facilities) {}

    public record InventoryView(String propertyId, List<BuildingView> buildings,
                                long totalBeds, long occupiedBeds, long availableBeds,
                                long reservedBeds, long maintenanceBeds, long unavailableBeds) {}
    public record BuildingView(String id, String name, String code, int floorCount, List<RoomView> rooms) {}
    public record RoomView(String id, int floorNumber, String roomNumber, SharingType sharingType,
                           long monthlyRent, List<String> facilities, RoomStatus status, List<BedView> beds) {}
    public record BedView(String id, String bedCode, BedStatus status, String bookingId, String tenancyId) {}
}
