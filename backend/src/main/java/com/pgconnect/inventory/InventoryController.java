package com.pgconnect.inventory;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner/properties/{propertyId}")
@PreAuthorize("hasRole('OWNER')")
public class InventoryController {
    private final InventoryService inventory;
    public InventoryController(InventoryService inventory) { this.inventory = inventory; }

    @GetMapping("/buildings")
    public List<Building> buildings(@PathVariable String propertyId, Authentication auth) {
        return inventory.listBuildings(auth.getName(), propertyId);
    }

    @PostMapping("/buildings")
    public Building createBuilding(@PathVariable String propertyId, @Valid @RequestBody InventoryService.BuildingRequest request, Authentication auth) {
        return inventory.createBuilding(auth.getName(), propertyId, request);
    }

    @GetMapping("/inventory")
    public InventoryService.InventoryView inventory(@PathVariable String propertyId, Authentication auth) {
        return inventory.inventory(auth.getName(), propertyId);
    }

    @PostMapping("/buildings/{buildingId}/rooms")
    public Room createRoom(@PathVariable String propertyId, @PathVariable String buildingId,
                           @Valid @RequestBody InventoryService.RoomRequest request, Authentication auth) {
        return inventory.createRoom(auth.getName(), propertyId, buildingId, request);
    }

    @PatchMapping("/beds/{bedId}/status")
    public Bed updateBed(@PathVariable String propertyId, @PathVariable String bedId,
                         @RequestParam BedStatus status, Authentication auth) {
        return inventory.updateBedStatus(auth.getName(), propertyId, bedId, status);
    }
}
